package com.verisure.backend.repository.projection;

import java.time.LocalDate;

/**
 * Fila de participación cerrada para el dashboard · {@code B1-07}.
 *
 * <p>Es la materia prima de {@code DashboardService}: de ella salen las horas,
 * los voluntarios distintos, las actividades, las entidades y los repartos por
 * departamento, organización, línea, modalidad y ubicación. Va con {@code Long}
 * en los contadores que Hibernate devuelve de {@code count}/{@code sum}, que
 * es un requisito de las expresiones de constructor.
 */
public record DashboardClosedRow(
        Long closureId,
        Long activityId,
        String activityTitle,
        Long userId,
        String department,
        String organization,
        String line,
        String mode,
        String location,
        Integer actualHours,
        LocalDate endDate,
        Long partnerId,
        String partnerName) {
}