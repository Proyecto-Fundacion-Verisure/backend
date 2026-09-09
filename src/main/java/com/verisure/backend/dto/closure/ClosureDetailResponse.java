package com.verisure.backend.dto.closure;

import java.time.Instant;

/**
 * Detalle de un cierre de participación. Accesible para administración o para
 * la persona propietaria.
 *
 * <p>No hay estados: la fila existe o no existe. Las horas que declara el
 * empleado son las definitivas ({@code actualHours}).
 *
 * <p>Tarea: B1-03.
 */
public record ClosureDetailResponse(
        Long id,
        Long registrationId,
        Long activityId,
        String activityTitle,
        Integer actualHours,
        Integer rating,
        String comment,
        String evidenceUrl,
        Instant submittedAt) {
}
