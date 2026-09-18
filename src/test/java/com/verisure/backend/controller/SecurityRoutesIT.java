package com.verisure.backend.controller;

import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.security.JwtService;
import com.verisure.backend.service.ActivityCatalogService;
import com.verisure.backend.service.ActivityService;
import com.verisure.backend.service.AuthService;
import com.verisure.backend.service.CsvExportService;
import com.verisure.backend.service.DashboardService;
import com.verisure.backend.service.NotificationService;
import com.verisure.backend.service.OrgActivityService;
import com.verisure.backend.service.PdfExportService;
import com.verisure.backend.service.RegistrationService;
import com.verisure.backend.service.SpotService;
import com.verisure.backend.support.AuthHelper;
import com.verisure.backend.support.ControllerTest;

/**
 * El reparto por rol de {@code SpringConfig}, tal como lo ve una petición.
 *
 * <p>Los servicios están mockeados y devuelven vacío: aquí solo importa el
 * código de estado que decide la cadena de seguridad antes de llegar a ellos.
 */
@ControllerTest({
        AuthController.class,
        ActivityCatalogController.class,
        ActivityController.class,
        RegistrationController.class,
        OrgActivityController.class,
        DashboardController.class })
class SecurityRoutesIT {

    private static final String AUTHORIZATION = "Authorization";

    @Autowired MockMvc mockMvc;
    @Autowired AuthHelper auth;
    @Autowired JwtService jwtService;

    @MockitoBean UserRepository userRepository;
    @MockitoBean AuthService authService;
    @MockitoBean ActivityCatalogService activityCatalogService;
    @MockitoBean ActivityService activityService;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean SpotService spotService;
    @MockitoBean OrgActivityService orgActivityService;
    @MockitoBean DashboardService dashboardService;
    @MockitoBean CsvExportService csvExportService;
    @MockitoBean PdfExportService pdfExportService;
    @MockitoBean NotificationService notificationService;

    @BeforeEach
    void emptyPages() {
        when(activityCatalogService.list(any(), any(), any())).thenReturn(Page.empty());
        when(activityService.list(any(), any())).thenReturn(Page.empty());
        when(orgActivityService.list(any(), any(), any())).thenReturn(Page.empty());
        when(registrationService.findMine(any())).thenReturn(List.of());
    }

    // Sin token

    @Test
    void login_isPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@verisure.ex\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void catalog_withoutToken_is401WithApiError() throws Exception {
        mockMvc.perform(get("/api/activities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/activities"));
    }

    @Test
    void uploads_withoutToken_isNot401() throws Exception {
        mockMvc.perform(get("/uploads/evidencias/no-existe.pdf"))
                .andExpect(status().is(not(401)));
    }

    // Token inválido

    @Test
    void expiredToken_is401() throws Exception {
        String expired = "Bearer " + new JwtService("clave-de-test-suficientemente-larga-para-hmac256-0123456789", -60_000L)
                .generateToken("ana.gil@verisure.ex", Role.EMPLOYEE);

        mockMvc.perform(get("/api/activities").header(AUTHORIZATION, expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedToken_is401() throws Exception {
        String valid = auth.bearer(Role.ADMIN);
        String tampered = valid.substring(0, valid.length() - 2) + "xx";

        mockMvc.perform(get("/api/dashboard").header(AUTHORIZATION, tampered))
                .andExpect(status().isUnauthorized());
    }

    // EMPLOYEE

    @Test
    void employee_cannotEnterAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/activities").header(AUTHORIZATION, auth.bearer(Role.EMPLOYEE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void employee_canReadCatalogAndOwnRegistrations() throws Exception {
        String bearer = auth.bearer(Role.EMPLOYEE);

        mockMvc.perform(get("/api/activities").header(AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/registrations/me").header(AUTHORIZATION, bearer))
                .andExpect(status().isOk());
    }

    @Test
    void employee_cannotEnterOrg() throws Exception {
        mockMvc.perform(get("/api/org/activities").header(AUTHORIZATION, auth.bearer(Role.EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    // PARTNER

    @Test
    void partner_cannotReadCatalog() throws Exception {
        mockMvc.perform(get("/api/activities").header(AUTHORIZATION, auth.bearer(Role.PARTNER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void partner_canReadOwnActivities() throws Exception {
        mockMvc.perform(get("/api/org/activities").header(AUTHORIZATION, auth.bearer(Role.PARTNER)))
                .andExpect(status().isOk());
    }

    @Test
    void partner_cannotUseEmployeeOnlyMethod() throws Exception {
        mockMvc.perform(get("/api/registrations/me").header(AUTHORIZATION, auth.bearer(Role.PARTNER)))
                .andExpect(status().isForbidden());
    }

    // ADMIN

    @Test
    void admin_canEnterAdminDashboardAndCatalog() throws Exception {
        String bearer = auth.bearer(Role.ADMIN);

        mockMvc.perform(get("/api/admin/activities").header(AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/dashboard").header(AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/activities").header(AUTHORIZATION, bearer))
                .andExpect(status().isOk());
    }

    @Test
    void admin_cannotEnterOrg() throws Exception {
        mockMvc.perform(get("/api/org/activities").header(AUTHORIZATION, auth.bearer(Role.ADMIN)))
                .andExpect(status().isForbidden());
    }
}
