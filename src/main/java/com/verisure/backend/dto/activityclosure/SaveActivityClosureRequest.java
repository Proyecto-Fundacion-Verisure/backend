package com.verisure.backend.dto.activityclosure;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Cuerpo de {@code PUT /api/admin/activities/{id}/closure}.
 *
 * <p>Los tres campos son opcionales (nullables) para que {@code @Valid} acepte
 * un borrador en blanco. Escala 1–5, la misma que
 * {@code CreateClosureRequest.rating}.
 *
 * <p>Tarea: B1-04.
 */
public record SaveActivityClosureRequest(
        @Min(1) @Max(5) Integer collaborationRating,
        String closingNotes,
        String lessonsLearned) {
}
