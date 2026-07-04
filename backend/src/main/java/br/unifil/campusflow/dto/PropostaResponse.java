package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.PropostaTroca;

public record PropostaResponse(
    Long id,
    Long reservaOrigemId,
    String salaNome,
    String dataHoraDesejada,
    String solicitanteNome,
    String donoNome,
    Long reservaOferecidaId,
    String salaOferecidaNome,
    String dataHoraOferecida,
    String justificativa,
    String status
) {
    public static PropostaResponse from(PropostaTroca p) {
        var r = p.getReservaOrigem();
        var o = p.getReservaOferecida();
        String dh = r.getDataReserva() + " as " + r.getHoraInicio();
        return new PropostaResponse(
            p.getId(),
            r.getId(),
            r.getSala().getNome(),
            dh,
            p.getUsuarioSolicitante().getNome(),
            r.getSolicitante().getNome(),
            o != null ? o.getId() : null,
            o != null ? o.getSala().getNome() : "N/A (proposta antiga)",
            o != null ? (o.getDataReserva() + " as " + o.getHoraInicio()) : "-",
            p.getJustificativa(),
            p.getStatus()
        );
    }
}
