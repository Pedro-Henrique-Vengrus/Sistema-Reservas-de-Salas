package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.PropostaTroca;

public record PropostaResponse(
    Long id,
    Long reservaOrigemId,
    String salaNome,
    String dataHoraDesejada,
    String solicitanteNome,
    String donoNome,
    String justificativa,
    String status
) {
    public static PropostaResponse from(PropostaTroca p) {
        var r = p.getReservaOrigem();
        String dh = r.getDataReserva() + " as " + r.getHoraInicio();
        return new PropostaResponse(
            p.getId(),
            r.getId(),
            r.getSala().getNome(),
            dh,
            p.getUsuarioSolicitante().getNome(),
            r.getSolicitante().getNome(),
            p.getJustificativa(),
            p.getStatus()
        );
    }
}
