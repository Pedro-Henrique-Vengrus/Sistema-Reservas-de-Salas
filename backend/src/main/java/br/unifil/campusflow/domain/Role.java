package br.unifil.campusflow.domain;

/**
 * Perfis do CampusFlow.
 *
 * Somente o ADMIN opera o painel administrativo. O REITOR e um solicitante como
 * o PROFESSOR: enxerga apenas os ambientes dos seus cursos, lanca reservas
 * proprias e participa de trocas.
 */
public enum Role {
    PROFESSOR,
    REITOR,
    ADMIN;

    /** Perfil que opera o painel administrativo (moderacao, CRUDs, relatorios). */
    public boolean ehAdministrativo() {
        return this == ADMIN;
    }

    /** Perfis que solicitam reservas em nome proprio. */
    public boolean ehSolicitante() {
        return this == PROFESSOR || this == REITOR;
    }
}
