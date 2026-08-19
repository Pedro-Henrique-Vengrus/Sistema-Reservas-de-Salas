package br.unifil.campusflow.domain;

import java.time.LocalTime;

/**
 * Turno academico derivado do horario de inicio da reserva.
 * Fonte unica da derivacao: nao duplicar esta regra em servicos ou no banco.
 */
public enum Turno {
    MATUTINO("Matutino"),
    VESPERTINO("Vespertino"),
    NOTURNO("Noturno");

    private final String rotulo;

    Turno(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public static Turno de(LocalTime horaInicio) {
        if (horaInicio.isBefore(LocalTime.of(12, 0))) return MATUTINO;
        if (horaInicio.isBefore(LocalTime.of(18, 0))) return VESPERTINO;
        return NOTURNO;
    }
}
