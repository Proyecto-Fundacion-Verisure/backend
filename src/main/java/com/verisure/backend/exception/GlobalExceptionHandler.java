package com.verisure.backend.exception;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import com.verisure.backend.validation.ValidDateRange;

/**
 * Traduce cualquier excepción a la forma única {@link ApiError}.
 *
 * <p>Un solo manejador cubre todos los códigos de negocio, porque
 * {@link DomainException} lleva su {@link ErrorCode} dentro. El resto de
 * métodos son para los errores genéricos de HTTP, que no son códigos de
 * dominio pero tienen que devolver el mismo cuerpo.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Todos los códigos de dominio, de golpe. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, WebRequest request) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.status())
                .body(ApiError.of(code.name(), ex.getMessage(), path(request)));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of("NOT_FOUND", ex.getMessage(), path(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     WebRequest request) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fields.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                      .add(error.getDefaultMessage()));
String dateRangeCode = ValidDateRange.class.getSimpleName();
        boolean dateRangeInvalid = ex.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> error.getCodes() != null
                        && Arrays.asList(error.getCodes()).contains(dateRangeCode));
        

        String code = dateRangeInvalid
                ? ErrorCode.INVALID_DATE_RANGE.name()
                : ErrorCode.VALIDATION_ERROR.name();
        String message = dateRangeInvalid
                ? "El rango de fechas no es válido"
                : "La solicitud no es válida";

        return ResponseEntity.badRequest().body(ApiError.of(code, message, path(request), fields));
    }

    /** Bean Validation sobre parámetros y variables de ruta. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex,
                                                     WebRequest request) {
        Map<String, List<String>> fields = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fields.computeIfAbsent(violation.getPropertyPath().toString(), k -> new ArrayList<>())
                      .add(violation.getMessage()));

        return ResponseEntity.badRequest().body(ApiError.of(
                ErrorCode.VALIDATION_ERROR.name(),
                "La solicitud no es válida", path(request), fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(WebRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                "MALFORMED_REQUEST", "El cuerpo de la petición no se puede leer", path(request)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(
                "UNAUTHORIZED", "Credenciales no válidas", path(request)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(
                "FORBIDDEN", "No tienes permiso para esta operación", path(request)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleTooLarge(WebRequest request) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiError.of(
                "PAYLOAD_TOO_LARGE", "El archivo supera el tamaño máximo", path(request)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiError.of(
                "UNSUPPORTED_MEDIA_TYPE", "Tipo de archivo no admitido", path(request)));
    }

    /**
     * Una URL que no corresponde a ningún endpoint.
     *
     * <p>Sin esto la recoge la red de seguridad de abajo y frontend recibe un
     * <b>500</b> por una simple errata en la ruta, con la traza registrada como
     * si fuera un fallo del servidor.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                "NOT_FOUND", "El recurso solicitado no existe", path(request)));
    }

    /**
     * Método HTTP que el endpoint no admite: {@code DELETE} sobre una ruta que
     * solo tiene {@code GET}, por ejemplo.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(WebRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiError.of(
                "METHOD_NOT_ALLOWED", "El método no está permitido en esta ruta", path(request)));
    }

    /**
     * Red de seguridad. Registra la traza pero <b>nunca</b> la devuelve:
     * {@code server.error.include-stacktrace=never} ya está puesto.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Error no controlado en {}", path(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                "INTERNAL_ERROR", "Error interno del servidor", path(request)));
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
