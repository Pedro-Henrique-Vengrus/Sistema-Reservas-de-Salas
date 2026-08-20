package br.unifil.campusflow.exception;

/** Falha de autorizacao de regra de negocio (visibilidade setorizada, perfil insuficiente) -> 403. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String msg) { super(msg); }
}
