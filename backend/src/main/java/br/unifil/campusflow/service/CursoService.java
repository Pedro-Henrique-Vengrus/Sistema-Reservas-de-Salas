package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.Curso;
import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.StatusRegistro;
import br.unifil.campusflow.domain.Usuario;
import br.unifil.campusflow.dto.CursoRequest;
import br.unifil.campusflow.dto.ImpactoResponse;
import br.unifil.campusflow.dto.ReservaResponse;
import br.unifil.campusflow.exception.ConfirmacaoNecessariaException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.CursoRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaService reservaService;
    private final UsuarioLogado usuarioLogado;

    public CursoService(CursoRepository cursoRepository,
                        ReservaRepository reservaRepository,
                        UsuarioRepository usuarioRepository,
                        ReservaService reservaService,
                        UsuarioLogado usuarioLogado) {
        this.cursoRepository = cursoRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.reservaService = reservaService;
        this.usuarioLogado = usuarioLogado;
    }

    /** Solicitantes so precisam do catalogo ativo; o painel administrativo ve todos os status. */
    @Transactional(readOnly = true)
    public List<Curso> listar(String termo, StatusRegistro status) {
        Usuario u = usuarioLogado.get();
        StatusRegistro efetivo = u.ehAdministrativo() ? status : StatusRegistro.ATIVO;
        return cursoRepository.buscar(termo, efetivo);
    }

    @Transactional(readOnly = true)
    public Curso buscarPorId(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso nao encontrado: " + id));
    }

    @Transactional
    public Curso criar(CursoRequest dto) {
        usuarioLogado.exigirAdministrativo();
        if (cursoRepository.existsByNomeIgnoreCase(dto.nome())) {
            throw new ConflitoException("Ja existe um curso com este nome.");
        }
        Curso c = new Curso();
        c.setNome(dto.nome());
        c.setSigla(dto.sigla());
        return cursoRepository.save(c);
    }

    @Transactional
    public Curso atualizar(Long id, CursoRequest dto) {
        usuarioLogado.exigirAdministrativo();
        Curso c = buscarPorId(id);
        if (cursoRepository.existsOutroComNome(dto.nome(), id)) {
            throw new ConflitoException("Ja existe outro curso com este nome.");
        }
        c.setNome(dto.nome());
        c.setSigla(dto.sigla());
        c.setDataModificacao(LocalDateTime.now());
        return cursoRepository.save(c);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida: ATIVO -> INATIVO -> exclusao fisica
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ImpactoResponse impacto(Long id) {
        usuarioLogado.exigirAdministrativo();
        Curso c = buscarPorId(id);
        List<Reserva> futuras = reservaRepository.findFuturasAtivasDoCurso(id, LocalDate.now());
        long usuariosVinculados = usuarioRepository.buscar(null, null, null, id).size();
        boolean podeExcluir = usuariosVinculados == 0 && futuras.isEmpty();
        String bloqueio = podeExcluir ? null : montarBloqueio(usuariosVinculados, futuras.size());
        return new ImpactoResponse(c.getId(), c.getNome(), futuras.size(), futuras.size(), usuariosVinculados,
                futuras.isEmpty(), podeExcluir, bloqueio,
                futuras.stream().map(ReservaResponse::from).toList());
    }

    /**
     * Cita apenas o que realmente prende o curso. Listar o que esta zerado
     * ("0 reserva(s) futura(s)") so confunde quem le o dialogo de exclusao.
     */
    private static String montarBloqueio(long usuarios, long reservasFuturas) {
        List<String> motivos = new ArrayList<>();
        if (usuarios > 0) {
            motivos.add(usuarios + (usuarios == 1 ? " usuario vinculado" : " usuarios vinculados"));
        }
        if (reservasFuturas > 0) {
            motivos.add(reservasFuturas + (reservasFuturas == 1 ? " reserva futura" : " reservas futuras"));
        }
        return "O curso ainda tem " + String.join(" e ", motivos) + ".";
    }

    /**
     * Exclusao logica. Um curso inativo deixa de conceder visibilidade sobre os ambientes.
     * Com reservas futuras ativas o sistema bloqueia, a menos que o administrador confirme
     * ({@code forcar}); nesse caso as reservas sao canceladas e os solicitantes notificados.
     */
    @Transactional
    public Curso inativar(Long id, boolean forcar) {
        usuarioLogado.exigirAdministrativo();
        Curso c = buscarPorId(id);
        if (!c.estaAtivo()) {
            throw new ConflitoException("Este curso ja esta inativo.");
        }

        List<Reserva> futuras = reservaRepository.findFuturasAtivasDoCurso(id, LocalDate.now());
        if (!futuras.isEmpty() && !forcar) {
            throw new ConfirmacaoNecessariaException(
                    "O curso \"" + c.getNome() + "\" tem " + futuras.size()
                  + " reserva(s) futura(s) ativa(s) de professores do curso. "
                  + "Confirme para inativar e cancelar essas reservas.",
                    impacto(id).comoDetalhes());
        }

        for (Reserva r : futuras) {
            reservaService.cancelarInterno(r, "O curso " + c.getNome() + " foi inativado pela administracao.");
        }

        c.setStatus(StatusRegistro.INATIVO);
        c.setDataDesativacao(LocalDateTime.now());
        c.setDataModificacao(LocalDateTime.now());
        return cursoRepository.save(c);
    }

    @Transactional
    public Curso reativar(Long id) {
        usuarioLogado.exigirAdministrativo();
        Curso c = buscarPorId(id);
        c.setStatus(StatusRegistro.ATIVO);
        c.setDataDesativacao(null);
        c.setDataModificacao(LocalDateTime.now());
        return cursoRepository.save(c);
    }

    /** Exclusao fisica: exige curso inativo, sem usuarios vinculados e sem reservas futuras. */
    @Transactional
    public void excluirFisicamente(Long id) {
        usuarioLogado.exigirAdministrativo();
        Curso c = buscarPorId(id);
        if (c.estaAtivo()) {
            throw new ConflitoException("Inative o curso antes de exclui-lo definitivamente.");
        }
        ImpactoResponse impacto = impacto(id);
        if (!impacto.podeExcluirFisicamente()) {
            throw new ConflitoException("Nao e possivel excluir o curso. " + impacto.bloqueio());
        }
        cursoRepository.delete(c);
    }
}
