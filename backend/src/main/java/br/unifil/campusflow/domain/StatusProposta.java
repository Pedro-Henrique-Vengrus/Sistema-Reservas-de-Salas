package br.unifil.campusflow.domain;

/**
 * Ciclo da proposta de troca.
 *
 * Mesmo dia e mesmo turno: PENDENTE -> ACEITA (o professor decide sozinho).
 * Fora do dia ou do turno: PENDENTE -> AGUARDANDO_GESTOR -> ACEITA,
 * porque a troca sai da rotina e exige aval administrativo.
 */
public enum StatusProposta {
    PENDENTE("Aguardando o professor"),
    AGUARDANDO_GESTOR("Aguardando o gestor"),
    ACEITA("Aceita"),
    RECUSADA("Recusada"),
    CANCELADA("Cancelada");

    private final String rotulo;

    StatusProposta(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    /** Ainda em tramitacao: nao foi efetivada nem encerrada. */
    public boolean emAberto() {
        return this == PENDENTE || this == AGUARDANDO_GESTOR;
    }
}
