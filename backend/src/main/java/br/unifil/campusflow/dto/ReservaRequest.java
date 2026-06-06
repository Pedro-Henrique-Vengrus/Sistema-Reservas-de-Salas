package br.unifil.campusflow.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRequest(
    @NotNull Long salaId,
    @NotNull LocalDate data,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFim,
    String tipoReserva
) {}
