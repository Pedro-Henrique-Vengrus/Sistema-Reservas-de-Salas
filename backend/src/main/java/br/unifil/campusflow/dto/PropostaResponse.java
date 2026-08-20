package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.PropostaTroca;
import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.StatusProposta;
import br.unifil.campusflow.domain.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public record PropostaResponse(
    Long id,
    StatusProposta status,
    String justificativa,
    LocalDate data,
    Turno turno,
    // Reserva desejada (do professor receptor)
    Long reservaOrigemId, String salaDesejada, LocalTime origemInicio, LocalTime origemFim,
    Long donoId, String donoNome,
    // Reserva oferecida (do proponente)
    Long reservaOferecidaId, String salaOferecida, LocalTime oferecidaInicio, LocalTime oferecidaFim,
    Long solicitanteId, String solicitanteNome
) {
    public static PropostaResponse from(PropostaTroca p) {
        Reserva origem = p.getReservaOrigem();
        Reserva oferecida = p.getReservaOferecida();
        return new PropostaResponse(
            p.getId(),
            p.getStatus(),
            p.getJustificativa(),
            origem.getDataReserva(),
            origem.getTurno(),
            origem.getId(), origem.getSala().getNome(), origem.getHoraInicio(), origem.getHoraFim(),
            origem.getSolicitante().getId(), origem.getSolicitante().getNome(),
            oferecida != null ? oferecida.getId() : null,
            oferecida != null ? oferecida.getSala().getNome() : null,
            oferecida != null ? oferecida.getHoraInicio() : null,
            oferecida != null ? oferecida.getHoraFim() : null,
            p.getUsuarioSolicitante().getId(), p.getUsuarioSolicitante().getNome()
        );
    }
}
