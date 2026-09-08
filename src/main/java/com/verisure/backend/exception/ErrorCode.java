package com.verisure.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Códigos de error de dominio del contrato de API.
 *
 * <p>Fuente única: nadie escribe el código como cadena suelta en su servicio.
 * Cada constante lleva su {@link HttpStatus}, de forma que
 * {@link GlobalExceptionHandler} necesita un solo manejador para todos.
 *
 * <p>Los errores genéricos de HTTP (401, 404, 413, 415, 500) <b>no</b> están
 * aquí: no son códigos de negocio. Los traduce el manejador global, que
 * devuelve un {@link ApiError} con la misma forma.
 */
public enum ErrorCode {

    // 400
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    DEADLINE_PASSED(HttpStatus.BAD_REQUEST),
    ACTIVITY_NOT_FINISHED(HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST),

    // 403
    NOT_OWNER(HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_VERIFIED(HttpStatus.FORBIDDEN),
    ACCOUNT_PENDING_APPROVAL(HttpStatus.FORBIDDEN),
    ACCOUNT_REJECTED(HttpStatus.FORBIDDEN),

    // 409
    ALREADY_REGISTERED(HttpStatus.CONFLICT),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    REGISTRATION_NOT_CONFIRMED(HttpStatus.CONFLICT),
    ACTIVITY_NOT_CLOSED(HttpStatus.CONFLICT),
    CLOSURE_ALREADY_CLOSED(HttpStatus.CONFLICT),
    ACTIVITY_FINISHED(HttpStatus.CONFLICT),
    ACTIVITY_NOT_EDITABLE(HttpStatus.CONFLICT),
    CIF_ALREADY_REGISTERED(HttpStatus.CONFLICT),
    PROPOSAL_ALREADY_DECIDED(HttpStatus.CONFLICT),

    // 410
    VERIFICATION_EXPIRED(HttpStatus.GONE),

    // 429
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
