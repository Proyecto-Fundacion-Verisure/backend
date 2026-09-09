package com.verisure.backend.dto.closure;

/**
 * Cuerpo de {@code POST /api/closures}, en la parte {@code request} del multipart.
 *
 * <p>{@code registrationId} va en el cuerpo porque el cierre todavía no existe
 * y no tiene identificador propio. {@code evidenceConsent} debe ser {@code true}
 * si se adjunta archivo en la parte {@code evidence}.
 *
 * <p>Tarea: B1-03.
 */
public record CreateClosureRequest(
        Long registrationId,
        Integer actualHours,
        Integer rating,
        String comment,
        Boolean evidenceConsent) {
}
