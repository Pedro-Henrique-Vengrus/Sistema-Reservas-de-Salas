package br.unifil.campusflow.domain;

import java.util.Set;

public enum StatusReserva {
    APROVADA("Aprovada"),
    PENDENTE_APROVACAO("Pendente de aprovacao"),
    RECUSADA("Recusada"),
    CANCELADA("Cancelada");

    /** Status que ocupam a sala e contam para conflito de horario. */
    public static final Set<StatusReserva> ATIVOS = Set.of(APROVADA, PENDENTE_APROVACAO);

    private final String rotulo;

    StatusReserva(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean ehAtiva() {
        return ATIVOS.contains(this);
    }
}
