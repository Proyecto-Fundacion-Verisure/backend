package com.verisure.backend.dto.org;

import java.time.LocalDate;

import com.verisure.backend.entity.enums.ActivityStatus;

/**
 * Una actividad vista por la entidad que la propone.
 *
 * <p>Sin datos de identidad de quien participa · {@code B1-19}: de las personas
 * apuntadas solo sale {@code occupiedSpots}. Y {@code reviewNote} es lo único
 * que distingue una devuelta de un borrador, porque no hay estado
 * {@code RETURNED}.
 *
 * <p>Dueña: BE3 · Tarea: B2-13.
 */
public record OrgActivityRow(
        Long id,
        String title,
        String line,
        String mode,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate registrationDeadline,
        Integer hours,
        Integer spots,
        Integer occupiedSpots,
        ActivityStatus status,
        String reviewNote) {
}
