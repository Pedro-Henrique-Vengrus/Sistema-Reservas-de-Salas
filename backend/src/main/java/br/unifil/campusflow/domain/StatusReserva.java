package br.unifil.campusflow.domain;

import java.util.Set;

public enum StatusReserva {
    APROVADA,
    PENDENTE_APROVACAO,
    RECUSADA,
    CANCELADA;

    /** Status que ocupam a sala e contam para conflito de horario. */
    public static final Set<StatusReserva> ATIVOS = Set.of(APROVADA, PENDENTE_APROVACAO);

    public boolean ehAtiva() {
        return ATIVOS.contains(this);
    }
}
