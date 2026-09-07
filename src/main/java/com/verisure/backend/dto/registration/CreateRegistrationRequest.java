package com.verisure.backend.dto.registration;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de POST /api/registrations.
 *
 * <p>Quién solicita sale del token, no del cuerpo: si viniera aquí, cualquiera
 * podría inscribir a otra persona.
 *
 * <p>Dueña: BE3 · Tarea: B3-02.
 */
public record CreateRegistrationRequest(@NotNull Long activityId) {
}
