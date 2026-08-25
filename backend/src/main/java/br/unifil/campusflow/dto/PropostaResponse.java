package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.PropostaTroca;
import br.unifil.campusflow.domain.Reserva;
import br.unifil.campusflow.domain.StatusProposta;
import br.unifil.campusflow.domain.Turno;
import br.unifil.campusflow.service.PropostaTrocaService;

import java.time.LocalDate;
import java.time.LocalTime;

public record PropostaResponse(
    Long id,
    StatusProposta status,
    String statusRotulo,
    // Fora do mesmo dia/turno a troca so se efetiva com o aval do gestor
    boolean exigeAvalDoGestor,
    String justificativa,
    // Reserva desejada (do professor receptor)
    Long reservaOrigemId, String salaDesejada,
    LocalDate origemData, Turno origemTurno, LocalTime origemInicio, LocalTime origemFim,
    Long donoId, String donoNome,
    // Reserva oferecida (do proponente)
    Long reservaOferecidaId, String salaOferecida,
    LocalDate oferecidaData, Turno oferecidaTurno, LocalTime oferecidaInicio, LocalTime oferecidaFim,
    Long solicitanteId, String solicitanteNome
) {
    public static PropostaResponse from(PropostaTroca p) {
        Reserva origem = p.getReservaOrigem();
        Reserva oferecida = p.getReservaOferecida();
        boolean exigeGestor = oferecida != null && PropostaTrocaService.exigeAvalDoGestor(origem, oferecida);
        return new PropostaResponse(
            p.getId(),
            p.getStatus(),
            p.getStatus().getRotulo(),
            exigeGestor,
            p.getJustificativa(),
            origem.getId(), origem.getSala().getNome(),
            origem.getDataReserva(), origem.getTurno(), origem.getHoraInicio(), origem.getHoraFim(),
            origem.getSolicitante().getId(), origem.getSolicitante().getNome(),
            oferecida != null ? oferecida.getId() : null,
            oferecida != null ? oferecida.getSala().getNome() : null,
            oferecida != null ? oferecida.getDataReserva() : null,
            oferecida != null ? oferecida.getTurno() : null,
            oferecida != null ? oferecida.getHoraInicio() : null,
            oferecida != null ? oferecida.getHoraFim() : null,
            p.getUsuarioSolicitante().getId(), p.getUsuarioSolicitante().getNome()
        );
    }
}
