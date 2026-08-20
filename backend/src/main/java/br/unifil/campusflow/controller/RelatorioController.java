package br.unifil.campusflow.controller;

import br.unifil.campusflow.domain.StatusReserva;
import br.unifil.campusflow.domain.TipoReserva;
import br.unifil.campusflow.domain.Turno;
import br.unifil.campusflow.dto.DashboardResponse;
import br.unifil.campusflow.service.RelatorioService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return relatorioService.dashboard(desde);
    }

    /** Exportacao tabular das reservas com os mesmos filtros da tela de relatorios. */
    @GetMapping(value = "/reservas.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long cursoId,
            @RequestParam(required = false) Long salaId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) StatusReserva status,
            @RequestParam(required = false) TipoReserva tipo,
            @RequestParam(required = false) Turno turno) {

        String csv = relatorioService.exportarCsv(inicio, fim, cursoId, salaId, usuarioId, status, tipo, turno);
        // BOM para o Excel abrir o arquivo em UTF-8 sem quebrar acentuacao
        byte[] corpo = ("﻿" + csv).getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reservas-campusflow.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(corpo);
    }
}
