package br.unifil.campusflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNotFound(RecursoNaoEncontradoException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null, null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiError> handleConflito(ConflitoException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null, null);
    }

    /** Acao com efeito colateral: devolve o impacto para o cliente confirmar e repetir com forcar=true. */
    @ExceptionHandler(ConfirmacaoNecessariaException.class)
    public ResponseEntity<ApiError> handleConfirmacao(ConfirmacaoNecessariaException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null, ex.getDetalhes());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ApiError> handleAcessoNegado(AcessoNegadoException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), null, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Email ou senha invalidos.", null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "Acesso negado.", null, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
            errors.put(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Erro de validacao.", errors, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), null, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String msg,
                                           Map<String, String> fieldErrors,
                                           Map<String, Object> detalhes) {
        ApiError err = new ApiError(LocalDateTime.now(), status.value(),
            status.getReasonPhrase(), msg, fieldErrors, detalhes);
        return ResponseEntity.status(status).body(err);
    }
}
