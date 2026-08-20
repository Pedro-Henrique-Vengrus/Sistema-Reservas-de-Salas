package br.unifil.campusflow.exception;

import java.util.Map;

/**
 * Acao bloqueada por ter efeitos colaterais (ex.: inativar sala com reservas futuras ativas).
 * Os {@code detalhes} descrevem o impacto para que o cliente peca confirmacao explicita
 * e repita a chamada com {@code forcar=true}.
 */
public class ConfirmacaoNecessariaException extends RuntimeException {

    private final transient Map<String, Object> detalhes;

    public ConfirmacaoNecessariaException(String msg, Map<String, Object> detalhes) {
        super(msg);
        this.detalhes = detalhes;
    }

    public Map<String, Object> getDetalhes() {
        return detalhes;
    }
}
