package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.PropostaRequest;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.PropostaTrocaRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Troca de salas entre professores.
 *
 * Pre-requisito da regra oficial: os dois professores ja precisam ter reservas ativas
 * e aprovadas no MESMO dia e no MESMO turno. A troca e mutua (cada um assume a reserva
 * do outro) e ambos recebem aviso.
 */
@Service
public class PropostaTrocaService {

    private final PropostaTrocaRepository propostaRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioLogado usuarioLogado;
    private final VisibilidadeService visibilidade;
    private final NotificacaoService notificacoes;

    public PropostaTrocaService(PropostaTrocaRepository propostaRepository,
                                ReservaRepository reservaRepository,
                                UsuarioLogado usuarioLogado,
                                VisibilidadeService visibilidade,
                                NotificacaoService notificacoes) {
        this.propostaRepository = propostaRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioLogado = usuarioLogado;
        this.visibilidade = visibilidade;
        this.notificacoes = notificacoes;
    }

    @Transactional(readOnly = true)
    public List<PropostaTroca> enviadas() {
        return propostaRepository.findEnviadas(usuarioLogado.get().getId());
    }

    @Transactional(readOnly = true)
    public List<PropostaTroca> recebidas() {
        return propostaRepository.findRecebidas(usuarioLogado.get().getId());
    }

    @Transactional(readOnly = true)
    public long pendentesRecebidas() {
        return propostaRepository.countPendentesRecebidas(usuarioLogado.get().getId());
    }

    @Transactional
    public PropostaTroca criar(PropostaRequest dto) {
        Usuario u = usuarioLogado.get();
        if (u.getRole() == Role.ADMIN) {
            throw new AcessoNegadoException("O perfil Admin nao participa de trocas de sala.");
        }

        Reserva desejada = carregar(dto.reservaOrigemId(), "Reserva desejada nao encontrada");
        Reserva oferecida = carregar(dto.reservaOferecidaId(), "Reserva oferecida nao encontrada");

        if (desejada.getId().equals(oferecida.getId())) {
            throw new ConflitoException("Selecione duas reservas diferentes.");
        }
        if (desejada.getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Voce nao pode propor troca da sua propria reserva.");
        }
        if (!oferecida.getSolicitante().getId().equals(u.getId())) {
            throw new ConflitoException("Voce so pode oferecer uma reserva sua.");
        }

        validarParDeReservas(desejada, oferecida);

        // Visibilidade setorizada nos dois sentidos: cada professor precisa enxergar o ambiente que vai assumir
        visibilidade.exigirAcessoASala(u, desejada.getSala());
        if (!visibilidade.podeVerSala(desejada.getSolicitante(), oferecida.getSala().getId())) {
            throw new ConflitoException("O ambiente que voce oferece nao pertence aos cursos de "
                    + desejada.getSolicitante().getNome() + ".");
        }

        validarConflitoDaTroca(desejada, oferecida);

        PropostaTroca p = new PropostaTroca();
        p.setReservaOrigem(desejada);
        p.setUsuarioSolicitante(u);
        p.setReservaOferecida(oferecida);
        p.setJustificativa(dto.justificativa());
        p.setStatus(StatusProposta.PENDENTE);
        PropostaTroca salva = propostaRepository.save(p);

        notificacoes.notificar(desejada.getSolicitante(), TipoNotificacao.TROCA_RECEBIDA,
                "Nova proposta de troca",
                u.getNome() + " quer trocar " + oferecida.getSala().getNome() + " pela sua reserva de "
                        + desejada.getSala().getNome() + " em " + ReservaService.descrever(desejada) + ".");
        notificacoes.notificar(u, TipoNotificacao.TROCA_RECEBIDA,
                "Proposta de troca enviada",
                "Sua proposta para " + desejada.getSolicitante().getNome() + " aguarda resposta.");
        return salva;
    }

    @Transactional
    public PropostaTroca responder(Long id, boolean aceitar) {
        Usuario u = usuarioLogado.get();
        PropostaTroca p = propostaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta nao encontrada"));

        // So o dono da reserva desejada pode responder
        if (!p.getReservaOrigem().getSolicitante().getId().equals(u.getId())) {
            throw new AcessoNegadoException("Apenas o dono da reserva pode responder a proposta.");
        }
        if (p.getStatus() != StatusProposta.PENDENTE) {
            throw new ConflitoException("Esta proposta ja foi respondida.");
        }

        Reserva desejada = p.getReservaOrigem();
        Reserva oferecida = p.getReservaOferecida();
        Usuario proponente = p.getUsuarioSolicitante();

        if (!aceitar) {
            p.setStatus(StatusProposta.RECUSADA);
            p.setDataModificacao(LocalDateTime.now());
            notificar(proponente, u, TipoNotificacao.TROCA_RECUSADA, "Proposta de troca recusada",
                    u.getNome() + " recusou sua proposta de troca.",
                    "Voce recusou a proposta de " + proponente.getNome() + ".");
            return propostaRepository.save(p);
        }

        if (oferecida == null) {
            throw new ConflitoException("Esta proposta nao tem reserva oferecida e nao pode ser aceita.");
        }
        // Estado pode ter mudado desde a criacao da proposta
        if (!oferecida.getSolicitante().getId().equals(proponente.getId())) {
            throw new ConflitoException("A reserva oferecida nao pertence mais ao proponente.");
        }
        validarParDeReservas(desejada, oferecida);
        validarConflitoDaTroca(desejada, oferecida);

        // Troca mutua: cada professor assume a reserva do outro
        Usuario dono = desejada.getSolicitante();
        desejada.setSolicitante(proponente);
        oferecida.setSolicitante(dono);
        desejada.setDataModificacao(LocalDateTime.now());
        oferecida.setDataModificacao(LocalDateTime.now());
        reservaRepository.save(desejada);
        reservaRepository.save(oferecida);

        invalidarPropostasConcorrentes(desejada.getId(), oferecida.getId(), p.getId());

        p.setStatus(StatusProposta.ACEITA);
        p.setDataModificacao(LocalDateTime.now());

        notificar(proponente, dono, TipoNotificacao.TROCA_ACEITA, "Troca de sala confirmada",
                dono.getNome() + " aceitou a troca: voce assume " + desejada.getSala().getNome()
                        + " em " + ReservaService.descrever(desejada) + ".",
                "Troca concluida: voce assume " + oferecida.getSala().getNome()
                        + " em " + ReservaService.descrever(oferecida) + ".");
        return propostaRepository.save(p);
    }

    /** O proponente pode retirar uma proposta que ainda nao foi respondida. */
    @Transactional
    public PropostaTroca cancelar(Long id) {
        Usuario u = usuarioLogado.get();
        PropostaTroca p = propostaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta nao encontrada"));

        if (!p.getUsuarioSolicitante().getId().equals(u.getId())) {
            throw new AcessoNegadoException("Apenas quem enviou a proposta pode cancela-la.");
        }
        if (p.getStatus() != StatusProposta.PENDENTE) {
            throw new ConflitoException("Esta proposta ja foi respondida.");
        }

        p.setStatus(StatusProposta.CANCELADA);
        p.setDataModificacao(LocalDateTime.now());
        notificacoes.notificar(p.getReservaOrigem().getSolicitante(), TipoNotificacao.TROCA_CANCELADA,
                "Proposta de troca cancelada",
                u.getNome() + " cancelou a proposta de troca enviada a voce.");
        return propostaRepository.save(p);
    }

    // ------------------------------------------------------------------
    // Regras
    // ------------------------------------------------------------------

    /** Pre-requisito oficial: ambas aprovadas, futuras, no mesmo dia e no mesmo turno. */
    private void validarParDeReservas(Reserva desejada, Reserva oferecida) {
        if (desejada.getStatus() != StatusReserva.APROVADA || oferecida.getStatus() != StatusReserva.APROVADA) {
            throw new ConflitoException("A troca so vale entre reservas aprovadas.");
        }
        LocalDate hoje = LocalDate.now();
        if (!desejada.ehFutura(hoje) || !oferecida.ehFutura(hoje)) {
            throw new ConflitoException("Nao e possivel trocar reservas de datas passadas.");
        }
        if (!desejada.getDataReserva().equals(oferecida.getDataReserva())) {
            throw new ConflitoException("A troca exige que as duas reservas sejam no mesmo dia.");
        }
        if (desejada.getTurno() != oferecida.getTurno()) {
            throw new ConflitoException("A troca exige que as duas reservas sejam no mesmo turno ("
                    + desejada.getTurno().getRotulo() + " x " + oferecida.getTurno().getRotulo() + ").");
        }
    }

    // Garante que, apos a troca, nenhum dos dois professores fique com dois compromissos sobrepostos
    private void validarConflitoDaTroca(Reserva desejada, Reserva oferecida) {
        boolean conflitoDono = reservaRepository.existeConflitoPessoal(
                desejada.getSolicitante().getId(), oferecida.getDataReserva(),
                oferecida.getHoraInicio(), oferecida.getHoraFim(), desejada.getId());
        boolean conflitoProponente = reservaRepository.existeConflitoPessoal(
                oferecida.getSolicitante().getId(), desejada.getDataReserva(),
                desejada.getHoraInicio(), desejada.getHoraFim(), oferecida.getId());
        if (conflitoDono || conflitoProponente) {
            throw new ConflitoException("A troca geraria conflito de horario para um dos professores.");
        }
    }

    // Uma vez efetivada a troca, qualquer outra proposta pendente que envolva as mesmas reservas
    // (como desejada ou oferecida) deixou de fazer sentido e e recusada automaticamente
    private void invalidarPropostasConcorrentes(Long idDesejada, Long idOferecida, Long idPropostaAceita) {
        List<PropostaTroca> concorrentes = propostaRepository.findPendentesEnvolvendo(
                List.of(idDesejada, idOferecida), idPropostaAceita);
        for (PropostaTroca c : concorrentes) {
            c.setStatus(StatusProposta.RECUSADA);
            c.setDataModificacao(LocalDateTime.now());
            notificacoes.notificar(c.getUsuarioSolicitante(), TipoNotificacao.TROCA_RECUSADA,
                    "Proposta de troca invalidada",
                    "Outra troca envolvendo a mesma reserva foi concluida antes da sua.");
        }
        propostaRepository.saveAll(concorrentes);
    }

    private Reserva carregar(Long id, String mensagemErro) {
        return reservaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(mensagemErro));
    }

    private void notificar(Usuario proponente, Usuario dono, TipoNotificacao tipo,
                           String titulo, String msgProponente, String msgDono) {
        notificacoes.notificar(proponente, tipo, titulo, msgProponente);
        notificacoes.notificar(dono, tipo, titulo, msgDono);
    }
}
