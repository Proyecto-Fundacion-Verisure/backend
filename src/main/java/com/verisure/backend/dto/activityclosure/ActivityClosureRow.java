package com.verisure.backend.dto.activityclosure;

import java.time.LocalDate;

/**
 * Fila de la cola de actividades pendientes de cierre
 * ({@code GET /api/admin/activities/pending-closure}).
 *
 * <p>Forma mínima acordada con BE1 (b1-04-plan.md §3): la bandeja lista
 * actividades, no cierres individuales. Los totales se ven en el detalle.
 * {@code partnerName} es {@code null} si la actividad no tiene partner.
 *
 * <p>Tarea: B1-04.
 */
public record ActivityClosureRow(
        Long activityId,
        String title,
        String partnerName,
        String line,
        LocalDate startDate,
        LocalDate endDate,
        Integer hours) {
}
