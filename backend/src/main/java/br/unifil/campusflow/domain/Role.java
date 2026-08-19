package br.unifil.campusflow.domain;

/**
 * Perfis do CampusFlow.
 *
 * O REITOR acumula os privilegios administrativos e tambem atua como solicitante
 * (cria reservas proprias), diferente do ADMIN, que administra o sistema.
 */
public enum Role {
    PROFESSOR,
    REITOR,
    ADMIN;

    /** Perfis que operam o painel administrativo (moderacao, CRUDs, relatorios). */
    public boolean ehAdministrativo() {
        return this == ADMIN || this == REITOR;
    }

    /** Perfis que solicitam reservas em nome proprio. */
    public boolean ehSolicitante() {
        return this == PROFESSOR || this == REITOR;
    }
}
