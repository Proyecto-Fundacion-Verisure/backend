package com.verisure.backend.repository.projection;

import java.time.Instant;

import com.verisure.backend.entity.enums.RegistrationStatus;

/**
 * Fila del tablero de administración de inscripciones.
 *
 * <p>{@code yearHours} es {@code Long} y no {@code Integer} porque en JPQL
 * {@code sum()} sobre un entero devuelve {@code Long}, y una expresión de
 * constructor exige que los tipos coincidan exactamente.
 *
 * <p>Dueña: BE3 · Tarea: B3-03.
 */
public record RegistrationRow(
        Long registrationId,
        String userName,
        String department,
        String organization,
        RegistrationStatus status,
        boolean accepted,
        Integer queuePosition,
        Long yearHours,
        Instant decidedAt,
        Instant createdAt) {
}
