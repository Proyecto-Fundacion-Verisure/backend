package com.verisure.backend.repository.projection;

import java.time.LocalDate;

import com.verisure.backend.entity.enums.ActivityStatus;

/**
 * Una actividad del catálogo, sin el corazón ni las plazas cubiertas.
 *
 * <p>Se proyecta en vez de devolver {@code Activity} para traer el nombre de la
 * entidad sin un {@code select} por fila y dejar que pagine la base de datos, que
 * es lo que un {@code join fetch} con {@code Pageable} impide.
 *
 * <p>Dueña: BE3 · Tarea: B2-07.
 */
public record ActivityCatalogRow(
        Long id,
        String title,
        String partnerName,
        String line,
        String mode,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        Integer hours,
        Integer spots,
        String imageUrl,
        ActivityStatus status) {
}
