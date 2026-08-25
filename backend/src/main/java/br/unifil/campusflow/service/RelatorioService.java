package br.unifil.campusflow.service;

import br.unifil.campusflow.domain.*;
import br.unifil.campusflow.dto.DashboardResponse;
import br.unifil.campusflow.dto.ReservaResponse;
import br.unifil.campusflow.repository.CursoRepository;
import br.unifil.campusflow.repository.ReservaRepository;
import br.unifil.campusflow.repository.SalaRepository;
import br.unifil.campusflow.repository.UsuarioRepository;
import br.unifil.campusflow.security.UsuarioLogado;
import br.unifil.campusflow.util.FormatoBr;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Metricas do dashboard e exportacao tabular das reservas. */
@Service
public class RelatorioService {

    private static final int LIMITE_RANKING = 8;
    private static final int LIMITE_PROXIMAS = 8;

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeriodoGradeService periodoGrade;
    private final UsuarioLogado usuarioLogado;

    public RelatorioService(ReservaRepository reservaRepository,
                            SalaRepository salaRepository,
                            CursoRepository cursoRepository,
                            UsuarioRepository usuarioRepository,
                            PeriodoGradeService periodoGrade,
                            UsuarioLogado usuarioLogado) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.periodoGrade = periodoGrade;
        this.usuarioLogado = usuarioLogado;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(LocalDate desde) {
        usuarioLogado.exigirAdministrativo();
        LocalDate inicio = desde != null ? desde : LocalDate.now().minusMonths(3);

        Map<StatusReserva, Long> porStatus = new EnumMap<>(StatusReserva.class);
        for (Object[] linha : reservaRepository.contarPorStatusDesde(inicio)) {
            porStatus.put((StatusReserva) linha[0], ((Number) linha[1]).longValue());
        }

        List<DashboardResponse.Contagem> porSala = reservaRepository.contarPorSalaDesde(inicio).stream()
                .limit(LIMITE_RANKING).map(DashboardResponse.Contagem::de).toList();
        List<DashboardResponse.Contagem> porCurso = reservaRepository.contarPorCursoDesde(inicio).stream()
                .limit(LIMITE_RANKING).map(DashboardResponse.Contagem::de).toList();

        List<ReservaResponse> proximas = reservaRepository
                .findProximasAprovadas(LocalDate.now(), PageRequest.of(0, LIMITE_PROXIMAS))
                .stream().map(ReservaResponse::from).toList();

        return new DashboardResponse(
                porStatus.getOrDefault(StatusReserva.APROVADA, 0L),
                porStatus.getOrDefault(StatusReserva.PENDENTE_APROVACAO, 0L),
                porStatus.getOrDefault(StatusReserva.RECUSADA, 0L),
                porStatus.getOrDefault(StatusReserva.CANCELADA, 0L),
                salaRepository.buscar(null, StatusRegistro.ATIVO, null, null, null).size(),
                cursoRepository.findByStatusOrderByNome(StatusRegistro.ATIVO).size(),
                usuarioRepository.countAtivos(),
                periodoGrade.gradeAberta(),
                porSala, porCurso, proximas);
    }

    /** Exportacao CSV da mesma consulta filtrada da tabela de relatorios. */
    @Transactional(readOnly = true)
    public String exportarCsv(LocalDate inicio, LocalDate fim, Long cursoId, Long salaId,
                              Long usuarioId, StatusReserva status, TipoReserva tipo, Turno turno) {
        usuarioLogado.exigirAdministrativo();
        var pagina = reservaRepository.buscarComFiltros(inicio, fim, cursoId, salaId, usuarioId, status, tipo, turno,
                PageRequest.of(0, 5000, Sort.by("dataReserva").descending().and(Sort.by("horaInicio"))));

        StringBuilder csv = new StringBuilder();
        csv.append("Data;Inicio;Fim;Turno;Ambiente;Codigo;Solicitante;Modo;Status\n");
        for (Reserva r : pagina.getContent()) {
            csv.append(FormatoBr.data(r.getDataReserva())).append(';')
               .append(FormatoBr.hora(r.getHoraInicio())).append(';')
               .append(FormatoBr.hora(r.getHoraFim())).append(';')
               .append(r.getTurno().getRotulo()).append(';')
               .append(escapar(r.getSala().getNome())).append(';')
               .append(escapar(r.getSala().getCodigo())).append(';')
               .append(escapar(r.getSolicitante().getNome())).append(';')
               .append(r.getTipoReserva().getRotulo()).append(';')
               .append(r.getStatus().getRotulo()).append('\n');
        }
        return csv.toString();
    }

    private String escapar(String valor) {
        if (valor == null) return "";
        return valor.replace(';', ',').replace('\n', ' ');
    }
}
