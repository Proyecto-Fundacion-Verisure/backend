package com.verisure.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verisure.backend.dto.org.OrgDashboardResponse;
import com.verisure.backend.dto.org.OrgDistributionEntry;
import com.verisure.backend.dto.org.OrgEvolutionEntry;
import com.verisure.backend.dto.org.OrgImpactVariations;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.service.OrgDashboardService;
import com.verisure.backend.support.AuthHelper;
import com.verisure.backend.support.ControllerTest;

/**
 * El panel de la entidad por HTTP · {@code B2-15}.
 *
 * <p>La cadena de seguridad es la real: aquí se comprueba que solo
 * {@code PARTNER} entra y que el año del filtro llega al servicio tal cual.
 */
@ControllerTest(OrgDashboardController.class)
class OrgDashboardControllerIT {

    private static final String URL = "/api/org/dashboard";
    private static final String PARTNER_EMAIL = "marta.ribas@caritasbcn.ex";

    @Autowired MockMvc mockMvc;
    @Autowired AuthHelper auth;

    @MockitoBean OrgDashboardService orgDashboardService;
    // `CustomAuthenticationManager` de la cadena de seguridad real lo necesita.
    @MockitoBean UserRepository userRepository;

    private OrgDashboardResponse response() {
        return new OrgDashboardResponse(
                2450,
                12,
                148,
                860,
                new OrgImpactVariations(9.2, 4, 6.7, -2.3),
                List.of(new OrgEvolutionEntry(2026, 2450)),
                List.of(new OrgDistributionEntry("desoledad", "Desoledad", 820)),
                LocalDate.of(2026, 11, 9));
    }

    @Test
    void partner_getsThePanel() throws Exception {
        when(orgDashboardService.getDashboard(eq(null), eq(PARTNER_EMAIL)))
                .thenReturn(response());

        mockMvc.perform(get(URL).header("Authorization", auth.bearer(PARTNER_EMAIL, Role.PARTNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivedHours").value(2450))
                .andExpect(jsonPath("$.benefitedPeople").value(860))
                .andExpect(jsonPath("$.variations.benefitedPeople").value(-2.3))
                .andExpect(jsonPath("$.evolutionByYear[0].year").value(2026))
                .andExpect(jsonPath("$.distributionByLine[0].id").value("desoledad"));
    }

    @Test
    void yearTravelsToTheService() throws Exception {
        when(orgDashboardService.getDashboard(eq(2026), eq(PARTNER_EMAIL)))
                .thenReturn(response());

        mockMvc.perform(get(URL).param("year", "2026")
                        .header("Authorization", auth.bearer(PARTNER_EMAIL, Role.PARTNER)))
                .andExpect(status().isOk());
    }

    @Test
    void employee_cannotReadThePartnerPanel() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", auth.bearer(Role.EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_cannotReadThePartnerPanel() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", auth.bearer(Role.ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_is401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
