package com.verisure.backend.dto.dashboard;

/**
 * Variación porcentual de cada KPI respecto al trimestre anterior.
 *
 * <p>Si el trimestre anterior no tiene datos, la variación es {@code 0} y no
 * {@code null}: el frontend pinta una flecha neutra en vez de un hueco.
 */
public record ImpactVariations(
        double reportedHours,
        double activeVolunteers,
        double finishedActivities,
        double activePartners) {

    public static ImpactVariations empty() {
        return new ImpactVariations(0, 0, 0, 0);
    }
}