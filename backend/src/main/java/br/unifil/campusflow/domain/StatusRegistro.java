package br.unifil.campusflow.domain;

/**
 * Ciclo de vida de cadastros com exclusao logica (Curso, Sala, Usuario):
 * ATIVO -> INATIVO (soft-delete) -> exclusao fisica.
 */
public enum StatusRegistro {
    ATIVO,
    INATIVO
}
