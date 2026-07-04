package br.unifil.campusflow.controller;

import br.unifil.campusflow.dto.ReservaRequest;
import br.unifil.campusflow.dto.ReservaResponse;
import br.unifil.campusflow.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public List<ReservaResponse> minhas() {
        return reservaService.minhasReservas().stream().map(ReservaResponse::from).toList();
    }

    @GetMapping("/outros")
    public List<ReservaResponse> outros() {
        return reservaService.reservasDeOutros().stream().map(ReservaResponse::from).toList();
    }

    @GetMapping("/agenda")
    public List<ReservaResponse> agenda(@RequestParam Long salaId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return reservaService.agendaDaSala(salaId, data).stream().map(ReservaResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@Valid @RequestBody ReservaRequest req) {
        return ReservaResponse.from(reservaService.criar(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        reservaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pendentes")
    public List<ReservaResponse> pendentes() {
        return reservaService.pendentes().stream().map(ReservaResponse::from).toList();
    }

    @GetMapping("/pendentes/count")
    public Map<String, Long> pendentesCount() {
        return Map.of("count", reservaService.countPendentes());
    }

    @PostMapping("/{id}/aprovar")
    public ReservaResponse aprovar(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.aprovar(id));
    }

    @PostMapping("/{id}/recusar")
    public ReservaResponse recusar(@PathVariable Long id) {
        return ReservaResponse.from(reservaService.recusar(id));
    }

    @GetMapping("/todas")
    public List<ReservaResponse> todas() {
        return reservaService.todas().stream().map(ReservaResponse::from).toList();
    }
}
