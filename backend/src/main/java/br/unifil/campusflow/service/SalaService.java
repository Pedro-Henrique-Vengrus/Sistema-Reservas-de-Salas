package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.ImpactoResponse;
import br.unifil.campusflow.dto.ReservaResponse;
import br.unifil.campusflow.dto.SalaRequest;
import br.unifil.campusflow.exception.ConfirmacaoNecessariaException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.CursoRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.SalaRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SalaService {

    private final SalaRepository salaRepository;
    private final CursoRepository cursoRepository;
    private final ReservaRepository reservaRepository;
    private final ReservaService reservaService;
    private final UsuarioLogado usuarioLogado;
    private final VisibilidadeService visibilidade;

    public SalaService(SalaRepository salaRepository,
                       CursoRepository cursoRepository,
                       ReservaRepository reservaRepository,
                       ReservaService reservaService,
                       UsuarioLogado usuarioLogado,
                       VisibilidadeService visibilidade) {
        this.salaRepository = salaRepository;
        this.cursoRepository = cursoRepository;
        this.reservaRepository = reservaRepository;
        this.reservaService = reservaService;
        this.usuarioLogado = usuarioLogado;
        this.visibilidade = visibilidade;
    }

    /**
     * Visibilidade setorizada aplicada pela role do usuario logado, nunca por parametro do cliente:
     * perfil administrativo ve o catalogo completo; solicitante ve apenas ambientes dos seus cursos;
     * solicitante sem curso ativo nao ve nenhum.
     */
    @Transactional(readOnly = true)
    public List<Sala> listar(String termo, StatusRegistro status, TipoAmbiente tipo,
                             Integer capacidadeMinima, Long cursoId) {
        Usuario u = usuarioLogado.get();
        if (visibilidade.veTodoOCatalogo(u)) {
            return salaRepository.buscar(termo, status, tipo, capacidadeMinima, cursoId);
        }
        Set<Long> cursoIds = visibilidade.cursoIdsDe(u);
        if (cursoIds.isEmpty()) return List.of();
        return salaRepository.buscarVisiveis(cursoIds, termo, tipo, capacidadeMinima, cursoId);
    }

    @Transactional(readOnly = true)
    public Sala buscarPorId(Long id) {
        Sala s = salaRepository.findByIdComCursos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ambiente nao encontrado: " + id));
        visibilidade.exigirAcessoASala(usuarioLogado.get(), s);
        return s;
    }

    @Transactional
    public Sala criar(SalaRequest dto) {
        usuarioLogado.exigirAdministrativo();
        Sala s = new Sala();
        aplicar(s, dto);
        return salaRepository.save(s);
    }

    @Transactional
    public Sala atualizar(Long id, SalaRequest dto) {
        usuarioLogado.exigirAdministrativo();
        Sala s = carregar(id);
        aplicar(s, dto);
        s.setDataModificacao(LocalDateTime.now());
        return salaRepository.save(s);
    }

    // ------------------------------------------------------------------
    // Ciclo de vida: ATIVO -> INATIVO -> exclusao fisica
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ImpactoResponse impacto(Long id) {
        usuarioLogado.exigirAdministrativo();
        Sala s = carregar(id);
        List<Reserva> futuras = reservaRepository.findFuturasAtivasDaSala(id, LocalDate.now());
        long totais = reservaRepository.countDaSala(id);
        boolean podeExcluir = totais == 0;
        String bloqueio = podeExcluir ? null
                : "O ambiente tem historico de reservas e nao pode ser excluido fisicamente; mantenha-o inativo.";
        return new ImpactoResponse(s.getId(), s.getNome(), futuras.size(), totais, 0,
                futuras.isEmpty(), podeExcluir, bloqueio,
                futuras.stream().map(ReservaResponse::from).toList());
    }

    /**
     * Exclusao logica. Com reservas futuras ativas a acao e bloqueada, a menos que o
     * administrador confirme explicitamente ({@code forcar}); nesse caso as reservas
     * futuras sao canceladas e os solicitantes notificados.
     */
    @Transactional
    public Sala inativar(Long id, boolean forcar) {
        usuarioLogado.exigirAdministrativo();
        Sala s = carregar(id);
        if (!s.estaAtiva()) {
            throw new ConflitoException("Este ambiente ja esta inativo.");
        }

        List<Reserva> futuras = reservaRepository.findFuturasAtivasDaSala(id, LocalDate.now());
        if (!futuras.isEmpty() && !forcar) {
            throw new ConfirmacaoNecessariaException(
                    "O ambiente \"" + s.getNome() + "\" tem " + futuras.size()
                  + " reserva(s) futura(s) ativa(s). Confirme para inativar e cancelar essas reservas.",
                    impacto(id).comoDetalhes());
        }

        for (Reserva r : futuras) {
            reservaService.cancelarInterno(r, "O ambiente " + s.getNome() + " foi inativado pela administracao.");
        }

        s.setStatus(StatusRegistro.INATIVO);
        s.setDataDesativacao(LocalDateTime.now());
        s.setDataModificacao(LocalDateTime.now());
        return salaRepository.save(s);
    }

    @Transactional
    public Sala reativar(Long id) {
        usuarioLogado.exigirAdministrativo();
        Sala s = carregar(id);
        s.setStatus(StatusRegistro.ATIVO);
        s.setDataDesativacao(null);
        s.setDataModificacao(LocalDateTime.now());
        return salaRepository.save(s);
    }

    /** Exclusao fisica: exige ambiente inativo e sem qualquer historico de reserva. */
    @Transactional
    public void excluirFisicamente(Long id) {
        usuarioLogado.exigirAdministrativo();
        Sala s = carregar(id);
        if (s.estaAtiva()) {
            throw new ConflitoException("Inative o ambiente antes de exclui-lo definitivamente.");
        }
        long totais = reservaRepository.countDaSala(id);
        if (totais > 0) {
            throw new ConflitoException("O ambiente tem " + totais
                    + " reserva(s) no historico e nao pode ser excluido. Mantenha-o inativo.");
        }
        salaRepository.delete(s);
    }

    // ------------------------------------------------------------------

    private Sala carregar(Long id) {
        return salaRepository.findByIdComCursos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ambiente nao encontrado: " + id));
    }

    private void aplicar(Sala s, SalaRequest dto) {
        s.setNome(dto.nome());
        s.setCodigo(dto.codigo());
        s.setTipo(dto.tipo());
        s.setCapacidade(dto.capacidade());
        s.setAndar(dto.andar());

        Set<Curso> cursos = new HashSet<>();
        for (Long cid : dto.cursoIds()) {
            Curso c = cursoRepository.findById(cid)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Curso nao encontrado: " + cid));
            cursos.add(c);
        }
        s.setCursos(cursos);
    }
}
