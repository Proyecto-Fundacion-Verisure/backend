package com.verisure.backend.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.org.OrgDashboardResponse;
import com.verisure.backend.dto.org.OrgDistributionEntry;
import com.verisure.backend.dto.org.OrgEvolutionEntry;
import com.verisure.backend.dto.org.OrgImpactVariations;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.ProposalRepository;
import com.verisure.backend.repository.projection.DashboardClosedRow;
import com.verisure.backend.repository.projection.OrgProposalScopeRow;

import lombok.RequiredArgsConstructor;

/**
 * Agregados del panel de una entidad · {@code B2-15}.
 *
 * <p>Se apoya en los mismos cierres de participación que el dashboard de la
 * Fundación —{@link DashboardClosedRow}—, filtrados por la entidad resuelta
 * desde el token. Las definiciones de «hora», «actividad» y «persona distinta»
 * son las mismas que allí a propósito: dos definiciones distintas acabarían
 * enseñando números distintos para el mismo dato.
 */
@Service
@RequiredArgsConstructor
public class OrgDashboardServiceImpl implements OrgDashboardService {

    private final ParticipationClosureRepository participationClosureRepository;
    private final ProposalRepository proposalRepository;
    private final PartnerResolver partnerResolver;

    @Override
    @Transactional(readOnly = true)
    public OrgDashboardResponse getDashboard(Integer year, String userEmail) {
        Partner partner = partnerResolver.resolve(userEmail);

        List<DashboardClosedRow> allRows =
                participationClosureRepository.findOrgDashboardData(partner.getId());
        List<OrgProposalScopeRow> scopes =
                proposalRepository.findAcceptedScopesByPartner(partner.getId());

        List<DashboardClosedRow> rows = rowsOfYear(allRows, year);
        List<OrgProposalScopeRow> scopesOfYear = scopesOfYear(scopes, year);

        return new OrgDashboardResponse(
                receivedHours(rows),
                distinctActivities(rows),
                distinctVolunteers(rows),
                benefitedPeople(scopesOfYear),
                variations(rows, scopesOfYear),
                evolutionByYear(allRows),
                distributionByLine(rows),
                LocalDate.now());
    }

    // ── KPIs ────────────────────────────────────────────────────────────────

    private List<DashboardClosedRow> rowsOfYear(List<DashboardClosedRow> rows, Integer year) {
        if (year == null) {
            return rows;
        }
        return rows.stream()
                .filter(row -> row.endDate() != null && row.endDate().getYear() == year)
                .toList();
    }

    private long receivedHours(List<DashboardClosedRow> rows) {
        return rows.stream()
                .mapToLong(row -> row.actualHours() == null ? 0L : row.actualHours())
                .sum();
    }

    private long distinctActivities(List<DashboardClosedRow> rows) {
        return rows.stream()
                .map(DashboardClosedRow::activityId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long distinctVolunteers(List<DashboardClosedRow> rows) {
        return rows.stream()
                .map(DashboardClosedRow::userId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private List<OrgProposalScopeRow> scopesOfYear(List<OrgProposalScopeRow> scopes, Integer year) {
        if (year == null) {
            return scopes;
        }
        return scopes.stream()
                .filter(row -> row.endDate() != null && row.endDate().getYear() == year)
                .toList();
    }

    private long benefitedPeople(List<OrgProposalScopeRow> scopes) {
        return scopes.stream()
                .mapToLong(row -> row.scope() == null ? 0L : row.scope())
                .sum();
    }

    // ── Variaciones año contra año ──────────────────────────────────────────

    /**
     * Compara el último año con datos contra el inmediatamente anterior.
     *
     * <p>Es año y no trimestre como en el dashboard de la Fundación: el panel de
     * la entidad se lee por años (su serie es anual) y la cabecera lo rotula
     * «respecto al año anterior».
     */
    private OrgImpactVariations variations(List<DashboardClosedRow> rows,
                                          List<OrgProposalScopeRow> scopes) {
        Map<Integer, List<DashboardClosedRow>> byYear = rows.stream()
                .filter(row -> row.endDate() != null)
                .collect(Collectors.groupingBy(row -> row.endDate().getYear()));

        if (byYear.isEmpty()) {
            return OrgImpactVariations.empty();
        }

        int currentYear = byYear.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
        List<DashboardClosedRow> currentRows = byYear.get(currentYear);
        List<DashboardClosedRow> previousRows = byYear.getOrDefault(currentYear - 1, List.of());
        List<OrgProposalScopeRow> currentScopes = scopesOfYear(scopes, currentYear);
        List<OrgProposalScopeRow> previousScopes = scopesOfYear(scopes, currentYear - 1);

        return new OrgImpactVariations(
                variation(receivedHours(currentRows), receivedHours(previousRows)),
                variation(distinctActivities(currentRows), distinctActivities(previousRows)),
                variation(distinctVolunteers(currentRows), distinctVolunteers(previousRows)),
                variation(benefitedPeople(currentScopes), benefitedPeople(previousScopes)));
    }

    private double variation(double current, double previous) {
        if (previous <= 0) {
            return 0;
        }
        return (current - previous) / previous * 100;
    }

    // ── Series ──────────────────────────────────────────────────────────────

    /**
     * Serie completa, sin el filtro de año: es la evolución, y recortarla al año
     * pedido dejaría un solo punto.
     */
    private List<OrgEvolutionEntry> evolutionByYear(List<DashboardClosedRow> rows) {
        Map<Integer, Long> hoursByYear = rows.stream()
                .filter(row -> row.endDate() != null)
                .collect(Collectors.groupingBy(
                        row -> row.endDate().getYear(),
                        Collectors.summingLong(row -> row.actualHours() == null ? 0L : row.actualHours())));

        return hoursByYear.entrySet().stream()
                .map(entry -> new OrgEvolutionEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(OrgEvolutionEntry::year))
                .toList();
    }

    private List<OrgDistributionEntry> distributionByLine(List<DashboardClosedRow> rows) {
        Map<String, Long> hoursByLine = rows.stream()
                .filter(row -> row.line() != null)
                .collect(Collectors.groupingBy(
                        DashboardClosedRow::line,
                        Collectors.summingLong(row -> row.actualHours() == null ? 0L : row.actualHours())));

        return hoursByLine.entrySet().stream()
                .map(entry -> new OrgDistributionEntry(
                        entry.getKey(),
                        DashboardLabels.line(entry.getKey()),
                        entry.getValue()))
                .sorted(Comparator.comparingLong(OrgDistributionEntry::value).reversed())
                .toList();
    }
}
