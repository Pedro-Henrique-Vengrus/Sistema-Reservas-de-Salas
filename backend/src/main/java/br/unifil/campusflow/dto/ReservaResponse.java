package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Reserva;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaResponse(
    Long id, Long salaId, String salaNome, String salaAndar,
    Long solicitanteId, String solicitanteNome,
    LocalDate data, LocalTime horaInicio, LocalTime horaFim,
    String tipoReserva, String status
) {
    public static ReservaResponse from(Reserva r) {
        return new ReservaResponse(
            r.getId(), r.getSala().getId(), r.getSala().getNome(), r.getSala().getAndar(),
            r.getSolicitante().getId(), r.getSolicitante().getNome(),
            r.getDataReserva(), r.getHoraInicio(), r.getHoraFim(),
            r.getTipoReserva(), r.getStatus()
        );
    }
}
