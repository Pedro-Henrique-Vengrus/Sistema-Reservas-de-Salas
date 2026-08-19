package br.unifil.campusflow.controller;

import br.unifil.campusflow.domain.StatusReserva;
import br.unifil.campusflow.domain.TipoReserva;
import br.unifil.campusflow.domain.Turno;
import br.unifil.campusflow.dto.ReservaRequest;
import br.unifil.campusflow.dto.ReservaResponse;
import br.unifil.campusflow.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping("/minhas")
    public List<ReservaResponse> minhas(@RequestParam(defaultValue = "false") boolean incluirHistorico) {
        return reservaService.minhasReservas(incluirHistorico).stream().map(ReservaResponse::from).toList();
    }

    /** Agenda de um intervalo para os ambientes informados (grade semanal do frontend). */
    @GetMapping("/agenda")
    public List<ReservaResponse> agenda(@RequestParam List<Long> salaIds,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return reservaService.agenda(salaIds, inicio, fim).stream().map(ReservaResponse::from).toList();
    }

    /** Reservas de outros professores que atendem ao pre-requisito de troca (mesmo dia e turno). */
    @GetMapping("/trocaveis")
    public List<ReservaResponse> trocaveis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Turno turno) {
        return reservaService.elegiveisParaTroca(data, turno).stream().map(ReservaResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@Valid @RequestBody ReservaRequest req) {
        return ReservaResponse.from(reservaService.criar(req));
    }

    @DeleteMapping("/{id}")
    public ReservaResponse cancelar(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.cancelar(id));
    }

    // ---------- Moderacao (Admin / Reitor) ----------

    @GetMapping("/moderacao")
    public List<ReservaResponse> moderacao() {
        return reservaService.filaModeracao().stream().map(ReservaResponse::from).toList();
    }

    @GetMapping("/moderacao/count")
    public Map<String, Long> moderacaoCount() {
        return Map.of("count", reservaService.countFilaModeracao());
    }

    @PostMapping("/{id}/aprovar")
    public ReservaResponse aprovar(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.aprovar(id));
    }

    @PostMapping("/{id}/recusar")
    public ReservaResponse recusar(@PathVariable Long id,
                                   @RequestBody(required = false) Map<String, String> corpo) {
        String motivo = corpo != null ? corpo.get("motivo") : null;
        return ReservaResponse.from(reservaService.recusar(id, motivo));
    }

    /** Busca filtrada e paginada: base da tabela de relatorios do painel. */
    @GetMapping
    public Page<ReservaResponse> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) StatusReserva status,
            @RequestParam(required = false) TipoReserva tipo,
            @RequestParam(required = false) Turno turno,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        var pageable = PageRequest.of(pagina, Math.min(tamanho, 200),
                Sort.by("dataReserva").descending().and(Sort.by("horaInicio").descending()));
        return reservaService.buscarComFiltros(inicio, fim, cursoId, salaId, usuarioId, status, tipo, turno, pageable)
                .map(ReservaResponse::from);
    }
}
