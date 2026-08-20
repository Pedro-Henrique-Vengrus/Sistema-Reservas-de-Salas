package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.TipoReserva;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRequest(
    @NotNull Long salaId,
    @NotNull LocalDate data,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFim,

    @NotNull(message = "Informe o modo da reserva (grade bimestral ou ultima hora)")
    TipoReserva tipoReserva,

    @Size(max = 300)
    String observacao,

    // Preenchido somente quando o painel administrativo reserva em nome de outro usuario
    Long usuarioId
) {}
