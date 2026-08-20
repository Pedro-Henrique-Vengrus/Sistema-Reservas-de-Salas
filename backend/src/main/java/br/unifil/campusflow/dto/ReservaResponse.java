package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.StatusReserva;
import br.unifil.campusflow.domain.TipoReserva;
import br.unifil.campusflow.domain.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaResponse(
    Long id,
    Long salaId, String salaNome, String salaCodigo, String salaAndar,
    Long solicitanteId, String solicitanteNome,
    LocalDate data, LocalTime horaInicio, LocalTime horaFim,
    Turno turno, TipoReserva tipoReserva, StatusReserva status,
    String observacao
) {
    public static ReservaResponse from(Reserva r) {
        return new ReservaResponse(
            r.getId(),
            r.getSala().getId(), r.getSala().getNome(), r.getSala().getCodigo(), r.getSala().getAndar(),
            r.getSolicitante().getId(), r.getSolicitante().getNome(),
            r.getDataReserva(), r.getHoraInicio(), r.getHoraFim(),
            r.getTurno(), r.getTipoReserva(), r.getStatus(), r.getObservacao()
        );
    }
}
