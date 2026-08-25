package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.ReservaRequest;
import br.unifil.campusflow.exception.AcessoNegadoException;
import br.unifil.campusflow.exception.ConflitoException;
import br.unifil.campusflow.exception.RecursoNaoEncontradoException;
import br.unifil.campusflow.repository.PropostaTrocaRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.SalaRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import br.unifil.campusflow.util.FormatoBr;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PropostaTrocaRepository propostaRepository;
    private final UsuarioLogado usuarioLogado;
    private final VisibilidadeService visibilidade;
    private final PeriodoGradeService periodoGrade;
    private final NotificacaoService notificacoes;

    public ReservaService(ReservaRepository reservaRepository,
                          SalaRepository salaRepository,
                          UsuarioRepository usuarioRepository,
                          PropostaTrocaRepository propostaRepository,
                          UsuarioLogado usuarioLogado,
                          VisibilidadeService visibilidade,
                          PeriodoGradeService periodoGrade,
                          NotificacaoService notificacoes) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.propostaRepository = propostaRepository;
        this.usuarioLogado = usuarioLogado;
        this.visibilidade = visibilidade;
        this.periodoGrade = periodoGrade;
        this.notificacoes = notificacoes;
    }

    // ------------------------------------------------------------------
    // Consultas do solicitante
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Reserva> minhasReservas(boolean incluirHistorico) {
        Long id = usuarioLogado.get().getId();
        return incluirHistorico
                ? reservaRepository.findTodasDoSolicitante(id)
                : reservaRepository.findMinhasAtivas(id);
    }

    /** Agenda de um intervalo, restrita aos ambientes visiveis ao usuario. */
    @Transactional(readOnly = true)
    public List<Reserva> agenda(Collection<Long> salaIds, LocalDate inicio, LocalDate fim) {
        Usuario u = usuarioLogado.get();
        List<Long> permitidas = salaIds.stream()
                .filter(id -> visibilidade.podeVerSala(u, id))
                .toList();
        if (permitidas.isEmpty()) return List.of();
        return reservaRepository.findAgenda(permitidas, inicio, fim);
    }

    /**
     * Reservas de outros professores elegiveis para proposta de troca: aprovadas, futuras,
     * em ambiente visivel e no mesmo dia/turno de alguma reserva aprovada do usuario.
     */
    @Transactional(readOnly = true)
    public List<Reserva> elegiveisParaTroca(LocalDate data, Turno turno) {
        Usuario u = usuarioLogado.get();
        Set<Long> cursoIds = visibilidade.cursoIdsDe(u);
        if (cursoIds.isEmpty()) return List.of();
        return reservaRepository.findElegiveisParaTroca(u.getId(), cursoIds, LocalDate.now(), data, turno);
    }

    // ------------------------------------------------------------------
    // Criacao
    // ------------------------------------------------------------------

    @Transactional
    public Reserva criar(ReservaRequest dto) {
        Usuario logado = usuarioLogado.get();
        Sala sala = salaRepository.findById(dto.salaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ambiente nao encontrado"));

        if (!sala.estaAtiva()) {
            throw new ConflitoException("Este ambiente esta inativo e nao aceita novas reservas.");
        }
        if (!dto.horaFim().isAfter(dto.horaInicio())) {
            throw new ConflitoException("O horario de termino deve ser posterior ao de inicio.");
        }
        if (dto.data().isBefore(LocalDate.now())) {
            throw new ConflitoException("Nao e possivel reservar uma data passada.");
        }

        Usuario solicitante = resolverSolicitante(logado, dto);

        // Regra central: o ambiente precisa pertencer aos cursos do solicitante
        visibilidade.exigirAcessoASala(solicitante, sala);

        if (reservaRepository.existeConflito(sala.getId(), dto.data(), dto.horaInicio(), dto.horaFim())) {
            throw new ConflitoException("Horario ja ocupado neste ambiente. Use a proposta de troca.");
        }

        StatusReserva status = definirStatusInicial(logado, dto.tipoReserva());

        Reserva r = new Reserva();
        r.setSolicitante(solicitante);
        r.setSala(sala);
        r.setDataReserva(dto.data());
        r.definirHorario(dto.horaInicio(), dto.horaFim());
        r.setTipoReserva(dto.tipoReserva());
        r.setStatus(status);
        r.setObservacao(dto.observacao());
        Reserva salva = reservaRepository.save(r);

        // Lancamento feito pelo painel em nome de outra pessoa: avisa quem recebeu a reserva
        if (!solicitante.getId().equals(logado.getId())) {
            notificacoes.notificar(solicitante, TipoNotificacao.RESERVA_CRIADA,
                    "Reserva lancada em seu nome",
                    logado.getNome() + " reservou " + sala.getNome() + " em " + descrever(salva) + ".");
        }
        return salva;
    }

    /**
     * Modo GRADE_BIMESTRAL: confirmacao direta, mas so enquanto o Admin mantem o periodo aberto.
     * Modo ULTIMA_HORA: sempre entra na fila de moderacao.
     * O painel administrativo lanca reservas ja aprovadas, inclusive fora do periodo da grade.
     */
    private StatusReserva definirStatusInicial(Usuario logado, TipoReserva tipo) {
        if (logado.getRole() == Role.ADMIN) {
            return StatusReserva.APROVADA;
        }
        if (tipo == TipoReserva.GRADE_BIMESTRAL) {
            if (!periodoGrade.gradeAberta()) {
                throw new ConflitoException(
                        "O preenchimento da grade bimestral esta fechado. "
                      + "Solicite como \"ultima hora\" ou aguarde a liberacao do Admin.");
            }
            return StatusReserva.APROVADA;
        }
        return StatusReserva.PENDENTE_APROVACAO;
    }

    private Usuario resolverSolicitante(Usuario logado, ReservaRequest dto) {
        boolean paraOutro = dto.usuarioId() != null && !dto.usuarioId().equals(logado.getId());
        if (!paraOutro) {
            if (logado.getRole() == Role.ADMIN) {
                throw new ConflitoException(
                        "O perfil Admin nao solicita reservas: selecione o usuario para quem o ambiente sera reservado.");
            }
            return logado;
        }
        if (!logado.ehAdministrativo()) {
            throw new AcessoNegadoException("Apenas o painel administrativo pode reservar em nome de outro usuario.");
        }
        Usuario alvo = usuarioRepository.findByIdComCursos(dto.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
        if (!alvo.estaAtivo()) {
            throw new ConflitoException("Este usuario esta inativo.");
        }
        return alvo;
    }

    // ------------------------------------------------------------------
    // Moderacao (Admin)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Reserva> filaModeracao() {
        usuarioLogado.exigirAdministrativo();
        return reservaRepository.findFilaModeracao();
    }

    @Transactional(readOnly = true)
    public long countFilaModeracao() {
        usuarioLogado.exigirAdministrativo();
        return reservaRepository.countFilaModeracao();
    }

    @Transactional
    public Reserva aprovar(Long id) {
        Usuario admin = usuarioLogado.exigirAdministrativo();
        Reserva r = carregarPendente(id);

        // O horario pode ter sido ocupado por outra reserva enquanto esta aguardava moderacao
        if (reservaRepository.existeConflitoExceto(r.getSala().getId(), r.getDataReserva(),
                r.getHoraInicio(), r.getHoraFim(), r.getId())) {
            throw new ConflitoException("O horario foi ocupado por outra reserva aprovada. Recuse esta solicitacao.");
        }

        r.setStatus(StatusReserva.APROVADA);
        r.setDataModificacao(LocalDateTime.now());
        Reserva salva = reservaRepository.save(r);

        notificacoes.notificar(r.getSolicitante(), TipoNotificacao.RESERVA_APROVADA,
                "Reserva aprovada",
                admin.getNome() + " aprovou sua reserva de " + r.getSala().getNome() + " em " + descrever(r) + ".");
        return salva;
    }

    @Transactional
    public Reserva recusar(Long id, String motivo) {
        Usuario admin = usuarioLogado.exigirAdministrativo();
        Reserva r = carregarPendente(id);

        r.setStatus(StatusReserva.RECUSADA);
        r.setDataModificacao(LocalDateTime.now());
        Reserva salva = reservaRepository.save(r);

        notificacoes.notificar(r.getSolicitante(), TipoNotificacao.RESERVA_RECUSADA,
                "Reserva recusada",
                admin.getNome() + " recusou sua reserva de " + r.getSala().getNome() + " em " + descrever(r)
                        + (motivo != null && !motivo.isBlank() ? ". Motivo: " + motivo : "."));
        return salva;
    }

    private Reserva carregarPendente(Long id) {
        Reserva r = reservaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));
        if (r.getStatus() != StatusReserva.PENDENTE_APROVACAO) {
            throw new ConflitoException("Esta reserva ja foi avaliada.");
        }
        return r;
    }

    // ------------------------------------------------------------------
    // Cancelamento
    // ------------------------------------------------------------------

    @Transactional
    public Reserva cancelar(Long id) {
        Usuario u = usuarioLogado.get();
        Reserva r = reservaRepository.findByIdComDados(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva nao encontrada"));

        boolean dono = r.getSolicitante().getId().equals(u.getId());
        if (!dono && !u.ehAdministrativo()) {
            throw new AcessoNegadoException("Voce so pode cancelar suas proprias reservas.");
        }
        if (!r.getStatus().ehAtiva()) {
            throw new ConflitoException("Esta reserva ja foi encerrada.");
        }

        Reserva salva = cancelarInterno(r, dono
                ? null
                : "Cancelada por " + u.getNome() + " pelo painel administrativo.");
        return salva;
    }

    /**
     * Cancelamento efetivo, com notificacao ao dono e invalidacao das propostas de troca pendentes.
     * Usado pelo cancelamento manual e pela inativacao forcada de ambiente/curso.
     */
    @Transactional
    public Reserva cancelarInterno(Reserva r, String avisoAoDono) {
        r.setStatus(StatusReserva.CANCELADA);
        r.setDataExclusao(LocalDateTime.now());
        r.setDataModificacao(LocalDateTime.now());
        Reserva salva = reservaRepository.save(r);

        invalidarPropostasDaReserva(r);

        if (avisoAoDono != null) {
            notificacoes.notificar(r.getSolicitante(), TipoNotificacao.RESERVA_CANCELADA,
                    "Reserva cancelada",
                    "Sua reserva de " + r.getSala().getNome() + " em " + descrever(r) + " foi cancelada. " + avisoAoDono);
        }
        return salva;
    }

    private void invalidarPropostasDaReserva(Reserva r) {
        List<PropostaTroca> pendentes = propostaRepository.findPendentesDaReserva(r.getId());
        for (PropostaTroca p : pendentes) {
            p.setStatus(StatusProposta.CANCELADA);
            p.setDataModificacao(LocalDateTime.now());
            notificacoes.notificar(p.getUsuarioSolicitante(), TipoNotificacao.TROCA_CANCELADA,
                    "Proposta de troca cancelada",
                    "Uma das reservas envolvidas na sua proposta foi cancelada.");
            notificacoes.notificar(p.getReservaOrigem().getSolicitante(), TipoNotificacao.TROCA_CANCELADA,
                    "Proposta de troca cancelada",
                    "Uma das reservas envolvidas em uma proposta recebida foi cancelada.");
        }
        propostaRepository.saveAll(pendentes);
    }

    // ------------------------------------------------------------------
    // Busca filtrada (tabelas e relatorios)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<Reserva> buscarComFiltros(LocalDate inicio, LocalDate fim, Long cursoId, Long salaId,
                                          Long usuarioId, StatusReserva status, TipoReserva tipo,
                                          Turno turno, Pageable pageable) {
        usuarioLogado.exigirAdministrativo();
        return reservaRepository.buscarComFiltros(inicio, fim, cursoId, salaId, usuarioId, status, tipo, turno, pageable);
    }

    /** Texto pt-BR da data e horario da reserva, usado nas notificacoes. */
    static String descrever(Reserva r) {
        return FormatoBr.periodo(r.getDataReserva(), r.getHoraInicio(), r.getHoraFim());
    }
}
