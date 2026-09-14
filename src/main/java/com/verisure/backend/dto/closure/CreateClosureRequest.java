package com.verisure.backend.dto.closure;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /api/closures}, en la parte {@code request} del multipart.
 *
 * <p>{@code registrationId} va en el cuerpo porque el cierre todavía no existe
 * y no tiene identificador propio. {@code evidenceConsent} debe ser {@code true}
 * si se adjunta archivo en la parte {@code evidence}.
 *
 * <p>{@code rating} va de 1 a 5, misma escala que
 * {@code ActivityClosure.collaborationRating}.
 *
 * <p>Tarea: B1-03.
 */
public record CreateClosureRequest(
        @NotNull Long registrationId,
        @NotNull @Min(1) Integer actualHours,
        @NotNull @Min(1) @Max(5) Integer rating,
        String comment,
        Boolean evidenceConsent) {
}
