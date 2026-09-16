package com.verisure.backend.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * Agregados del dashboard de la Fundación · {@code B1-07}.
 *
 * <p>Todo se calcula sobre participaciones {@code CLOSED}. Las cuatro cifras
 * de la cabecera —horas, voluntarios, actividades, entidades— son las que pinta
 * el frontend en la fila de KPIs, y cada una responde a una definición distinta:
 *
 * <ul>
 *   <li>{@code reportedHours}: horas declaradas en los cierres, sumadas.</li>
 *   <li>{@code activeVolunteers}: personas distintas con participación cerrada.</li>
 *   <li>{@code finishedActivities}: actividades distintas con participación cerrada.</li>
 *   <li>{@code activePartners}: entidades distintas con participación cerrada
 *       (las que la Fundación publica por su cuenta no tienen entidad y no cuentan).</li>
 * </ul>
 */
public record DashboardResponse(
        long reportedHours,
        long activeVolunteers,
        long finishedActivities,
        long activePartners,
        ImpactVariations impactVariations,
        List<EffectivenessMetric> effectiveness,
        List<DepartmentEntry> participationByDepartment,
        List<DistributionEntry> distributionByMode,
        List<DistributionEntry> distributionByLocation,
        List<FavoriteRankingEntry> favoriteRanking,
        LocalDate generatedAt) {

    public static DashboardResponse empty(ImpactVariations variations) {
        return new DashboardResponse(
                0, 0, 0, 0,
                variations,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                LocalDate.now());
    }
}