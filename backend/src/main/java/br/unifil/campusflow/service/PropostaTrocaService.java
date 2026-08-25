package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.PropostaRequest;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.PropostaTrocaRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Troca de salas entre professores.
 *
 * Troca de rotina — mesmo dia e mesmo turno — e resolvida direto entre os dois
 * professores: o dono da reserva aceita e a troca se efetiva na hora.
 * Fora do dia ou do turno, a troca sai da rotina: depois do aceite do professor
 * ela ainda precisa do aval do gestor, que decide no painel de moderacao.
 *
 * A troca e sempre mutua (cada um assume a reserva do outro) e os dois lados
 * sao notificados em cada etapa.
 */
@Service
public class PropostaTrocaService {

    private final PropostaTrocaRepository propostaRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;
    private final VisibilidadeService visibilidade;
    private final NotificacaoService notificacoes;

    public PropostaTrocaService(PropostaTrocaRepository propostaRepository,
                                ReservaRepository reservaRepository,
                                UsuarioRepository usuarioRepository,
                                UsuarioLogado usuarioLogado,
                                VisibilidadeService visibilidade,
                                NotificacaoService notificacoes) {
        this.propostaRepository = propostaRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
        this.visibilidade = visibilidade;
        this.notificacoes = notificacoes;
    }

    /** Troca de rotina dispensa o gestor; qualquer desvio de dia ou turno exige. */
    public static boolean exigeAvalDoGestor(Reserva desejada, Reserva oferecida) {
        return !desejada.getDataReserva().equals(oferecida.getDataReserva())
                || desejada.getTurno() != oferecida.getTurno();
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

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

    /** Fila do gestor: trocas ja aceitas pelo professor, aguardando o aval administrativo. */
    @Transactional(readOnly = true)
    public List<PropostaTroca> filaDoGestor() {
        usuarioLogado.exigirAdministrativo();
        return propostaRepository.findAguardandoGestor();
    }

    @Transactional(readOnly = true)
    public long countFilaDoGestor() {
        usuarioLogado.exigirAdministrativo();
        return propostaRepository.countAguardandoGestor();
    }

    // ------------------------------------------------------------------
    // Criacao
    // ------------------------------------------------------------------

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

        String aviso = exigeAvalDoGestor(desejada, oferecida)
                ? " Como e fora do mesmo dia/turno, apos o seu aceite a troca ainda passa pelo gestor."
                : "";
        notificacoes.notificar(desejada.getSolicitante(), TipoNotificacao.TROCA_RECEBIDA,
                "Nova proposta de troca",
                u.getNome() + " quer trocar " + oferecida.getSala().getNome() + " pela sua reserva de "
                        + desejada.getSala().getNome() + " em " + ReservaService.descrever(desejada) + "." + aviso);
        notificacoes.notificar(u, TipoNotificacao.TROCA_RECEBIDA,
                "Proposta de troca enviada",
                "Sua proposta para " + desejada.getSolicitante().getNome() + " aguarda resposta.");
        return salva;
    }

    // ------------------------------------------------------------------
    // Resposta do professor dono da reserva
    // ------------------------------------------------------------------

    @Transactional
    public PropostaTroca responder(Long id, boolean aceitar) {
        Usuario u = usuarioLogado.get();
        PropostaTroca p = carregarProposta(id);

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
        revalidar(p, desejada, oferecida, proponente);

        // Fora do mesmo dia/turno o aceite do professor nao efetiva: encaminha ao gestor
        if (exigeAvalDoGestor(desejada, oferecida)) {
            p.setStatus(StatusProposta.AGUARDANDO_GESTOR);
            p.setDataModificacao(LocalDateTime.now());
            avisarGestores(p);
            notificar(proponente, u, TipoNotificacao.TROCA_AGUARDA_GESTOR, "Troca encaminhada ao gestor",
                    u.getNome() + " aceitou a troca. Como e fora do mesmo dia/turno, ela aguarda o aval do gestor.",
                    "Voce aceitou a troca. Como e fora do mesmo dia/turno, ela aguarda o aval do gestor.");
            return propostaRepository.save(p);
        }

        efetivarTroca(p, desejada, oferecida, proponente, u);
        return propostaRepository.save(p);
    }

    // ------------------------------------------------------------------
    // Decisao do gestor (trocas fora do mesmo dia/turno)
    // ------------------------------------------------------------------

    @Transactional
    public PropostaTroca decidirComoGestor(Long id, boolean aprovar, String motivo) {
        Usuario gestor = usuarioLogado.exigirAdministrativo();
        PropostaTroca p = carregarProposta(id);

        if (p.getStatus() != StatusProposta.AGUARDANDO_GESTOR) {
            throw new ConflitoException("Esta proposta nao esta aguardando o aval do gestor.");
        }

        Reserva desejada = p.getReservaOrigem();
        Reserva oferecida = p.getReservaOferecida();
        Usuario proponente = p.getUsuarioSolicitante();
        Usuario dono = desejada.getSolicitante();

        if (!aprovar) {
            p.setStatus(StatusProposta.RECUSADA);
            p.setDataModificacao(LocalDateTime.now());
            String complemento = motivo != null && !motivo.isBlank() ? " Motivo: " + motivo : "";
            notificar(proponente, dono, TipoNotificacao.TROCA_RECUSADA, "Troca recusada pelo gestor",
                    gestor.getNome() + " recusou a troca." + complemento,
                    gestor.getNome() + " recusou a troca." + complemento);
            return propostaRepository.save(p);
        }

        // O cenario pode ter mudado entre o aceite do professor e a decisao do gestor
        revalidar(p, desejada, oferecida, proponente);
        efetivarTroca(p, desejada, oferecida, proponente, dono);
        return propostaRepository.save(p);
    }

    /** O proponente pode retirar uma proposta que ainda nao foi concluida. */
    @Transactional
    public PropostaTroca cancelar(Long id) {
        Usuario u = usuarioLogado.get();
        PropostaTroca p = carregarProposta(id);

        if (!p.getUsuarioSolicitante().getId().equals(u.getId())) {
            throw new AcessoNegadoException("Apenas quem enviou a proposta pode cancela-la.");
        }
        if (!p.getStatus().emAberto()) {
            throw new ConflitoException("Esta proposta ja foi concluida.");
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

    /** Vale para qualquer troca: reservas aprovadas e futuras. Dia e turno definem quem aprova. */
    private void validarParDeReservas(Reserva desejada, Reserva oferecida) {
        if (desejada.getStatus() != StatusReserva.APROVADA || oferecida.getStatus() != StatusReserva.APROVADA) {
            throw new ConflitoException("A troca so vale entre reservas aprovadas.");
        }
        LocalDate hoje = LocalDate.now();
        if (!desejada.ehFutura(hoje) || !oferecida.ehFutura(hoje)) {
            throw new ConflitoException("Nao e possivel trocar reservas de datas passadas.");
        }
    }

    /** Revalida o estado antes de efetivar ou encaminhar: o cenario pode ter mudado. */
    private void revalidar(PropostaTroca p, Reserva desejada, Reserva oferecida, Usuario proponente) {
        if (!oferecida.getSolicitante().getId().equals(proponente.getId())) {
            throw new ConflitoException("A reserva oferecida nao pertence mais ao proponente.");
        }
        validarParDeReservas(desejada, oferecida);
        validarConflitoDaTroca(desejada, oferecida);
    }

    /** Efetiva a troca mutua e encerra as propostas concorrentes. */
    private void efetivarTroca(PropostaTroca p, Reserva desejada, Reserva oferecida,
                               Usuario proponente, Usuario dono) {
        Usuario donoOriginal = desejada.getSolicitante();
        desejada.setSolicitante(proponente);
        oferecida.setSolicitante(donoOriginal);
        desejada.setDataModificacao(LocalDateTime.now());
        oferecida.setDataModificacao(LocalDateTime.now());
        reservaRepository.save(desejada);
        reservaRepository.save(oferecida);

        invalidarPropostasConcorrentes(desejada.getId(), oferecida.getId(), p.getId());

        p.setStatus(StatusProposta.ACEITA);
        p.setDataModificacao(LocalDateTime.now());

        notificar(proponente, donoOriginal, TipoNotificacao.TROCA_ACEITA, "Troca de sala confirmada",
                "Troca concluida: voce assume " + desejada.getSala().getNome()
                        + " em " + ReservaService.descrever(desejada) + ".",
                "Troca concluida: voce assume " + oferecida.getSala().getNome()
                        + " em " + ReservaService.descrever(oferecida) + ".");
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

    // Uma vez efetivada a troca, qualquer outra proposta em aberto que envolva as mesmas reservas
    // (como desejada ou oferecida) deixou de fazer sentido e e recusada automaticamente
    private void invalidarPropostasConcorrentes(Long idDesejada, Long idOferecida, Long idPropostaAceita) {
        List<PropostaTroca> concorrentes = propostaRepository.findEmAbertoEnvolvendo(
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

    private void avisarGestores(PropostaTroca p) {
        String resumo = p.getUsuarioSolicitante().getNome() + " e "
                + p.getReservaOrigem().getSolicitante().getNome()
                + " acertaram uma troca fora do mesmo dia/turno.";
        for (Usuario gestor : usuarioRepository.findAdministradoresAtivos()) {
            notificacoes.notificar(gestor, TipoNotificacao.TROCA_AGUARDA_GESTOR,
                    "Troca aguardando seu aval", resumo);
        }
    }

    private PropostaTroca carregarProposta(Long id) {
        return propostaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Proposta nao encontrada"));
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
