package br.unifil.campusflow.domain;

/**
 * Modos de reserva.
 *
 * GRADE_BIMESTRAL: preenchimento liberado periodicamente pelo Admin (ver {@link PeriodoGrade});
 * confirma direto quando nao ha conflito de sala/horario.
 * ULTIMA_HORA: eventos e aulas extras; sempre entra na fila de moderacao.
 */
public enum TipoReserva {
    GRADE_BIMESTRAL("Grade Bimestral"),
    ULTIMA_HORA("Ultima Hora");

    private final String rotulo;

    TipoReserva(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
