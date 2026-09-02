package com.verisure.backend.dto.closure;

/**
 * Cuerpo de {@code POST /api/closures}, en la parte {@code request} del multipart.
 *
 * <p>TODO C-03 · campos: {@code registrationId}, {@code actualHours},
 * {@code rating} (1..5), {@code comment?} y {@code evidenceConsent}. Se
 * concretan al escribir 03-contrato-api.md con frontend delante.
 */
public record CreateClosureRequest() {
}
