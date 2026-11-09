package com.verisure.backend.dto.org;

/**
 * Variación porcentual de los KPIs de una entidad respecto al trimestre
 * anterior · {@code B2-15}.
 *
 * <p>Misma regla que en el dashboard de la Fundación: si el trimestre previo no
 * tiene datos, la variación es {@code 0} y no {@code null}, para que el
 * frontend pinte una flecha neutra en vez de un hueco.
 */
public record OrgImpactVariations(
        double receivedHours,
        double activities,
        double distinctVolunteers,
        double benefitedPeople) {

    public static OrgImpactVariations empty() {
        return new OrgImpactVariations(0, 0, 0, 0);
    }
}
