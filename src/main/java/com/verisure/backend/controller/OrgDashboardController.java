package com.verisure.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.org.OrgDashboardResponse;
import com.verisure.backend.service.OrgDashboardService;

import lombok.RequiredArgsConstructor;

/**
 * El panel de impacto de la entidad · {@code B2-15}.
 *
 * <p>No lleva {@code @PreAuthorize}: la cadena de seguridad ya reserva
 * {@code /api/org/**} al rol {@code PARTNER}.
 *
 * <p>No recibe identificador de entidad. Lo resuelve el servicio desde el
 * token; ver {@code PartnerResolver}.
 *
 * <p>Dueña: BE3.
 */
@RestController
@RequestMapping("/api/org/dashboard")
@RequiredArgsConstructor
public class OrgDashboardController {

    private final OrgDashboardService orgDashboardService;

    /** Agregados del panel, opcionalmente filtrados por año de la actividad. */
    @GetMapping
    public OrgDashboardResponse dashboard(
            @RequestParam(required = false) Integer year,
            Authentication authentication) {

        return orgDashboardService.getDashboard(year, authentication.getName());
    }
}
