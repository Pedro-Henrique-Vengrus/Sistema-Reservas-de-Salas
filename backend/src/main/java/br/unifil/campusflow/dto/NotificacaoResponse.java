package br.unifil.campusflow.dto;

import br.unifil.campusflow.domain.Notificacao;
import br.unifil.campusflow.domain.TipoNotificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
    Long id, TipoNotificacao tipo, String titulo, String mensagem,
    Boolean lida, LocalDateTime dataCriacao
) {
    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(),
            n.getLida(), n.getDataCriacao());
    }
}
