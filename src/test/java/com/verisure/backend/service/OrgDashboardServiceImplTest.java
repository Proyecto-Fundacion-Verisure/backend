package com.verisure.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.verisure.backend.dto.org.OrgDashboardResponse;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.ProposalRepository;
import com.verisure.backend.repository.projection.DashboardClosedRow;
import com.verisure.backend.repository.projection.OrgProposalScopeRow;

/**
 * Agregados del panel de la entidad · {@code B2-15}.
 *
 * <p>Los cierres y los alcances declarados llegan mockeados: aquí se comprueba
 * la aritmética y las reglas del contrato (qué cuenta cada KPI, cómo se filtra
 * por año y qué se compara con el trimestre anterior).
 */
@ExtendWith(MockitoExtension.class)
class OrgDashboardServiceImplTest {

    private static final String EMAIL = "marta.ribas@caritasbcn.ex";

    @Mock ParticipationClosureRepository participationClosureRepository;
    @Mock ProposalRepository proposalRepository;
    @Mock PartnerResolver partnerResolver;

    @InjectMocks OrgDashboardServiceImpl service;

    private void partnerWithId(long id) {
        Partner partner = new Partner();
        partner.setId(id);
        when(partnerResolver.resolve(EMAIL)).thenReturn(partner);
    }

    private DashboardClosedRow row(long activityId, long userId, Integer hours, LocalDate endDate,
                                   String line) {
        return new DashboardClosedRow(
                1L, activityId, "Actividad", userId, "Tecnología", "VERISURE_ES",
                line, "PRESENCIAL", "Madrid", hours, endDate, 7L, "Cáritas");
    }

    private OrgProposalScopeRow scope(Integer scope, LocalDate endDate) {
        return new OrgProposalScopeRow(scope, endDate);
    }

    @Test
    void sumaHorasYCuentaActividadesYPersonasDistintas() {
        partnerWithId(7L);
        when(participationClosureRepository.findOrgDashboardData(7L)).thenReturn(List.of(
                row(10, 1, 4, LocalDate.of(2026, 3, 1), "educar"),
                row(10, 2, 6, LocalDate.of(2026, 3, 1), "educar"),
                row(11, 1, 5, LocalDate.of(2026, 6, 1), "desoledad")));
        when(proposalRepository.findAcceptedScopesByPartner(7L)).thenReturn(List.of(
                scope(120, LocalDate.of(2026, 6, 1)),
                scope(80, LocalDate.of(2026, 3, 1))));

        OrgDashboardResponse response = service.getDashboard(null, EMAIL);

        assertThat(response.receivedHours()).isEqualTo(15);
        assertThat(response.activities()).isEqualTo(2);
        assertThat(response.distinctVolunteers()).isEqualTo(2);
        assertThat(response.benefitedPeople()).isEqualTo(200);
    }

    @Test
    void filtraPorAnioDeLaActividadPeroLaSerieSigueCompleta() {
        partnerWithId(7L);
        when(participationClosureRepository.findOrgDashboardData(7L)).thenReturn(List.of(
                row(10, 1, 4, LocalDate.of(2025, 3, 1), "educar"),
                row(11, 2, 6, LocalDate.of(2026, 6, 1), "desoledad")));
        when(proposalRepository.findAcceptedScopesByPartner(7L)).thenReturn(List.of(
                scope(120, LocalDate.of(2026, 6, 1)),
                scope(80, LocalDate.of(2025, 3, 1))));

        OrgDashboardResponse response = service.getDashboard(2026, EMAIL);

        assertThat(response.receivedHours()).isEqualTo(6);
        assertThat(response.activities()).isEqualTo(1);
        assertThat(response.distinctVolunteers()).isEqualTo(1);
        assertThat(response.benefitedPeople()).isEqualTo(120);
        assertThat(response.evolutionByYear())
                .extracting(entry -> entry.year(), entry -> entry.receivedHours())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2025, 4L),
                        org.assertj.core.groups.Tuple.tuple(2026, 6L));
    }

    @Test
    void comparaElUltimoTrimestreConElAnterior() {
        partnerWithId(7L);
        when(participationClosureRepository.findOrgDashboardData(7L)).thenReturn(List.of(
                row(10, 1, 10, LocalDate.of(2026, 1, 15), "educar"),
                row(11, 2, 20, LocalDate.of(2026, 4, 15), "educar")));
        when(proposalRepository.findAcceptedScopesByPartner(7L)).thenReturn(List.of(
                scope(100, LocalDate.of(2026, 1, 15)),
                scope(150, LocalDate.of(2026, 4, 15))));

        OrgDashboardResponse response = service.getDashboard(null, EMAIL);

        assertThat(response.variations().receivedHours()).isEqualTo(100);
        assertThat(response.variations().benefitedPeople()).isEqualTo(50);
    }

    @Test
    void reparteLasHorasPorLineaDeMayorAMenor() {
        partnerWithId(7L);
        when(participationClosureRepository.findOrgDashboardData(7L)).thenReturn(List.of(
                row(10, 1, 4, LocalDate.of(2026, 3, 1), "medioambiente"),
                row(11, 2, 9, LocalDate.of(2026, 3, 1), "desoledad")));
        when(proposalRepository.findAcceptedScopesByPartner(7L)).thenReturn(List.of());

        OrgDashboardResponse response = service.getDashboard(null, EMAIL);

        assertThat(response.distributionByLine())
                .extracting(entry -> entry.id(), entry -> entry.label(), entry -> entry.value())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("desoledad", "Desoledad", 9L),
                        org.assertj.core.groups.Tuple.tuple(
                                "medioambiente", "Medio ambiente", 4L));
    }

    @Test
    void devuelveUnPanelVacioSinDatos() {
        partnerWithId(7L);
        when(participationClosureRepository.findOrgDashboardData(7L)).thenReturn(List.of());
        when(proposalRepository.findAcceptedScopesByPartner(7L)).thenReturn(List.of());

        OrgDashboardResponse response = service.getDashboard(null, EMAIL);

        assertThat(response.receivedHours()).isZero();
        assertThat(response.activities()).isZero();
        assertThat(response.distinctVolunteers()).isZero();
        assertThat(response.benefitedPeople()).isZero();
        assertThat(response.variations().receivedHours()).isZero();
        assertThat(response.evolutionByYear()).isEmpty();
        assertThat(response.distributionByLine()).isEmpty();
    }
}
