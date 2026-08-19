package br.unifil.campusflow.domain;

/** Catalogo de tipos de ambiente reservavel. */
public enum TipoAmbiente {
    SALA_AULA("Sala de Aula"),
    LAB_INFORMATICA("Laboratorio de Informatica"),
    LAB_CIENCIAS("Laboratorio de Ciencias"),
    AUDITORIO("Auditorio"),
    OUTRO("Outro");

    private final String rotulo;

    TipoAmbiente(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
