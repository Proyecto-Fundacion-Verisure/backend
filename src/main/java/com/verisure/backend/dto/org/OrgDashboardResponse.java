package com.verisure.backend.dto.org;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel de impacto de una entidad · {@code B2-15}.
 *
 * <p>Sale de las mismas participaciones {@code CLOSED} que el dashboard de la
 * Fundación, filtradas por la entidad de la sesión. Las cuatro cifras de la
 * cabecera responden a definiciones distintas:
 *
 * <ul>
 *   <li>{@code receivedHours}: horas declaradas en los cierres, sumadas.</li>
 *   <li>{@code activities}: actividades distintas con participación cerrada.</li>
 *   <li>{@code distinctVolunteers}: personas distintas que han participado.</li>
 *   <li>{@code benefitedPeople}: suma de {@code scope} de las propuestas
 *       aceptadas. Es gente alcanzada, no voluntariado: no sale de los cierres.</li>
 * </ul>
 *
 * <p>Ningún campo identifica a una persona: ni nombres, ni correos, ni horas
 * individuales. Es la misma barrera de {@code B1-19} que el resto de
 * {@code /api/org/**}.
 */
public record OrgDashboardResponse(
        long receivedHours,
        long activities,
        long distinctVolunteers,
        long benefitedPeople,
        OrgImpactVariations variations,
        List<OrgEvolutionEntry> evolutionByYear,
        List<OrgDistributionEntry> distributionByLine,
        LocalDate generatedAt) {

    public static OrgDashboardResponse empty() {
        return new OrgDashboardResponse(
                0, 0, 0, 0,
                OrgImpactVariations.empty(),
                List.of(), List.of(),
                LocalDate.now());
    }
}
