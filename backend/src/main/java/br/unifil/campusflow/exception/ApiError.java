package br.unifil.campusflow.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    Map<String, String> fieldErrors,
    // Preenchido quando a acao exige confirmacao explicita (ver ConfirmacaoNecessariaException)
    Map<String, Object> detalhes
) {}
