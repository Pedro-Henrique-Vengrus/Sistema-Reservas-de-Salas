package br.unifil.campusflow.controller;

import br.unifil.campusflow.dto.PeriodoGradeRequest;
import br.unifil.campusflow.dto.PeriodoGradeResponse;
import br.unifil.campusflow.service.PeriodoGradeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Liberacao periodica do preenchimento da grade bimestral (regra do edital). */
@RestController
@RequestMapping("/api/periodo-grade")
public class PeriodoGradeController {

    private final PeriodoGradeService periodoGradeService;

    public PeriodoGradeController(PeriodoGradeService periodoGradeService) {
        this.periodoGradeService = periodoGradeService;
    }

    /** Qualquer autenticado consulta: o formulario de reserva depende deste estado. */
    @GetMapping
    public PeriodoGradeResponse obter() {
        return PeriodoGradeResponse.from(periodoGradeService.obter());
    }

    @GetMapping("/aberta")
    public Map<String, Boolean> aberta() {
        return Map.of("aberta", periodoGradeService.gradeAberta());
    }

    @PutMapping
    public PeriodoGradeResponse alterar(@Valid @RequestBody PeriodoGradeRequest req) {
        return PeriodoGradeResponse.from(periodoGradeService.alterar(req));
    }
}
