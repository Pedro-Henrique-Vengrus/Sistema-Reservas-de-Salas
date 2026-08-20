package br.unifil.campusflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PeriodoGradeRequest(
    @NotNull(message = "Informe se o preenchimento da grade esta aberto")
    Boolean aberto,

    @Size(max = 120)
    String descricao,

    LocalDate inicioVigencia,
    LocalDate fimVigencia
) {}
