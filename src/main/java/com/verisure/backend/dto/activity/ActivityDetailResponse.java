package com.verisure.backend.dto.activity;

import java.time.LocalDate;

import com.verisure.backend.entity.enums.ActivityStatus;

/**
 * La ficha de una actividad: todo lo de la tarjeta, más lo que solo tiene
 * sentido con la actividad abierta.
 *
 * <p>{@code registrationDeadline} va aquí y no en la tarjeta porque es la fecha
 * que decide si el botón de apuntarse sigue vivo, y esa decisión se toma
 * leyendo la ficha.
 */
public record ActivityDetailResponse(
        Long id,
        String title,
        String partnerName,
        String line,
        String mode,
        String location,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate registrationDeadline,
        Integer hours,
        Integer spots,
        Integer occupiedSpots,
        String imageUrl,
        ActivityStatus status,
        boolean favoritedByMe) {
}
