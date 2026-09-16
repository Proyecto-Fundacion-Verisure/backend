package com.verisure.backend.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.dashboard.DashboardResponse;
import com.verisure.backend.dto.dashboard.DepartmentEntry;
import com.verisure.backend.dto.dashboard.DistributionEntry;
import com.verisure.backend.dto.dashboard.EffectivenessMetric;
import com.verisure.backend.dto.dashboard.FavoriteRankingEntry;
import com.verisure.backend.dto.dashboard.ImpactVariations;
import com.verisure.backend.dto.dashboard.ParticipationEntry;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.FavoriteRepository;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.DashboardClosedRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int RANKING_SIZE = 10;

    private final ParticipationClosureRepository participationClosureRepository;
    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final FavoriteRepository favoriteRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Integer year, String line) {
        List<DashboardClosedRow> rows = participationClosureRepository.findDashboardData(year, line);

        if (rows.isEmpty()) {
            return DashboardResponse.empty(ImpactVariations.empty());
        }

        return new DashboardResponse(
                reportedHours(rows),
                activeVolunteers(rows),
                finishedActivities(rows),
                activePartners(rows),
                computeVariations(rows),
                effectiveness(rows, year, line),
                byDepartment(rows),
                byOrganization(rows),
                byLine(rows),
                byMode(rows),
                byLocation(rows),
                ranking(year, line),
                LocalDate.now());
    }

    // ── KPIs ────────────────────────────────────────────────────────────────

    private long reportedHours(List<DashboardClosedRow> rows) {
        return rows.stream().mapToLong(r -> r.actualHours() == null ? 0L : r.actualHours()).sum();
    }

    private long activeVolunteers(List<DashboardClosedRow> rows) {
        return rows.stream().map(DashboardClosedRow::userId)
                .filter(Objects::nonNull).distinct().count();
    }

    private long finishedActivities(List<DashboardClosedRow> rows) {
        return rows.stream().map(DashboardClosedRow::activityId)
                .filter(Objects::nonNull).distinct().count();
    }

    private long activePartners(List<DashboardClosedRow> rows) {
        return rows.stream().map(DashboardClosedRow::partnerId)
                .filter(Objects::nonNull).distinct().count();
    }

    // ── Variaciones por trimestre ───────────────────────────────────────────

    /**
     * Compara el último trimestre con datos contra el inmediatamente anterior.
     *
     * <p>El frontend las pinta como «respecto al trimestre anterior». Si no hay
     * trimestre previo, la variación es {@code 0} y no {@code null}: una flecha
     * neutra en vez de un hueco.
     */
    private ImpactVariations computeVariations(List<DashboardClosedRow> rows) {
        Map<Quarter, List<DashboardClosedRow>> byQuarter = rows.stream()
                .filter(r -> r.endDate() != null)
                .collect(Collectors.groupingBy(r -> Quarter.of(r.endDate())));
        if (byQuarter.isEmpty()) {
            return ImpactVariations.empty();
        }

        Quarter current = byQuarter.keySet().stream().max(Comparator.naturalOrder()).orElseThrow();
        List<DashboardClosedRow> currentRows = byQuarter.get(current);
        List<DashboardClosedRow> previousRows = byQuarter.getOrDefault(current.previous(), List.of());

        return new ImpactVariations(
                variation(reportedHours(currentRows), reportedHours(previousRows)),
                variation(activeVolunteers(currentRows), activeVolunteers(previousRows)),
                variation(finishedActivities(currentRows), finishedActivities(previousRows)),
                variation(activePartners(currentRows), activePartners(previousRows)));
    }

    private double variation(double current, double previous) {
        if (previous <= 0) {
            return 0;
        }
        return (current - previous) / previous * 100;
    }

    private record Quarter(int year, int quarter) implements Comparable<Quarter> {

        static Quarter of(LocalDate date) {
            return new Quarter(date.getYear(), ((date.getMonthValue() - 1) / 3) + 1);
        }

        Quarter previous() {
            int q = quarter - 1;
            int y = year;
            if (q == 0) {
                q = 4;
                y -= 1;
            }
            return new Quarter(y, q);
        }

        @Override
        public int compareTo(Quarter other) {
            int byYear = Integer.compare(year, other.year);
            return byYear != 0 ? byYear : Integer.compare(quarter, other.quarter);
        }
    }

    // ── Eficacia ────────────────────────────────────────────────────────────

    private List<EffectivenessMetric> effectiveness(List<DashboardClosedRow> rows, Integer year, String line) {
        long closed = rows.size();
        long employees = userRepository.countByRole(Role.EMPLOYEE);
        long spots = activityRepository.sumSpotsInScope(year, line);
        long registrations = registrationRepository.countAllRegistrationsInScope(year, line);

        return List.of(
                EffectivenessMetric.of("workforce-participation", "Participación de la plantilla",
                        activeVolunteers(rows), employees),
                EffectivenessMetric.of("place-occupancy", "Ocupación de plazas", closed, spots),
                EffectivenessMetric.of("registration-conversion", "Inscripción → participación",
                        closed, registrations));
    }

    // ── Distribuciones ──────────────────────────────────────────────────────

    private List<DepartmentEntry> byDepartment(List<DashboardClosedRow> rows) {
        Map<String, Set<Long>> byDepartment = rows.stream()
                .filter(r -> r.department() != null)
                .collect(Collectors.groupingBy(DashboardClosedRow::department,
                        Collectors.mapping(DashboardClosedRow::userId, Collectors.toSet())));

        return byDepartment.entrySet().stream()
                .map(e -> new DepartmentEntry(e.getKey(), e.getValue().size()))
                .sorted(Comparator.comparingLong(DepartmentEntry::participants).reversed())
                .toList();
    }

    /**
     * Personas distintas por organización.
     *
     * <p>Las cuentas de entidad no tienen organización, así que el filtro de
     * nulos ya las deja fuera, como pide el contrato.
     */
    private List<ParticipationEntry> byOrganization(List<DashboardClosedRow> rows) {
        return participantsBy(rows, DashboardClosedRow::organization, DashboardLabels::organization);
    }

    /** Personas distintas por línea de acción. */
    private List<ParticipationEntry> byLine(List<DashboardClosedRow> rows) {
        return participantsBy(rows, DashboardClosedRow::line, DashboardLabels::line);
    }

    private List<ParticipationEntry> participantsBy(List<DashboardClosedRow> rows,
                                                    Function<DashboardClosedRow, String> key,
                                                    Function<String, String> label) {
        Map<String, Set<Long>> byKey = rows.stream()
                .filter(r -> key.apply(r) != null)
                .collect(Collectors.groupingBy(key,
                        Collectors.mapping(DashboardClosedRow::userId, Collectors.toSet())));

        return byKey.entrySet().stream()
                .map(e -> new ParticipationEntry(e.getKey(), label.apply(e.getKey()), e.getValue().size()))
                .sorted(Comparator.comparingLong(ParticipationEntry::participants).reversed())
                .toList();
    }

    private List<DistributionEntry> byMode(List<DashboardClosedRow> rows) {
        Map<String, Long> counts = rows.stream()
                .filter(r -> r.mode() != null)
                .collect(Collectors.groupingBy(DashboardClosedRow::mode, Collectors.counting()));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            return List.of();
        }

        return counts.entrySet().stream()
                .map(e -> toModeEntry(e.getKey(), e.getValue() * 100.0 / total))
                .sorted(Comparator.comparingDouble(DistributionEntry::value).reversed())
                .toList();
    }

    private DistributionEntry toModeEntry(String mode, double percentage) {
        return new DistributionEntry(DashboardLabels.modeId(mode), DashboardLabels.mode(mode), percentage);
    }

    private List<DistributionEntry> byLocation(List<DashboardClosedRow> rows) {
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.location() == null || r.location().isBlank() ? "Sin ubicación" : r.location(),
                        Collectors.counting()));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            return List.of();
        }

        return counts.entrySet().stream()
                .map(e -> new DistributionEntry(locationId(e.getKey()), e.getKey(), e.getValue() * 100.0 / total))
                .sorted(Comparator.comparingDouble(DistributionEntry::value).reversed())
                .toList();
    }

    private String locationId(String location) {
        return "Sin ubicación".equals(location)
                ? "sin-ubicacion"
                : location.toLowerCase().replace(' ', '-');
    }

    // ── Demanda ─────────────────────────────────────────────────────────────

    private List<FavoriteRankingEntry> ranking(Integer year, String line) {
        return favoriteRepository.findTopRanking(year, line, PageRequest.of(0, RANKING_SIZE));
    }
}