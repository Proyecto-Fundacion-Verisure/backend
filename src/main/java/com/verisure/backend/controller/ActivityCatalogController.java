package com.verisure.backend.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.activity.ActivityCardResponse;
import com.verisure.backend.dto.activity.ActivityDetailResponse;
import com.verisure.backend.dto.activity.CatalogFilters;
import com.verisure.backend.service.ActivityCatalogService;

import lombok.RequiredArgsConstructor;

/**
 * El catálogo de actividades: la rejilla y la ficha.
 *
 * <p>No lleva {@code @PreAuthorize}: la cadena de seguridad ya reserva estas dos
 * rutas a {@code EMPLOYEE} y {@code ADMIN}, así que una entidad colaboradora
 * recibe 403 sin llegar hasta aquí. Lo suyo lo ve en {@code /api/org/activities}.
 *
 * <p>Va aparte de {@link ActivityController}, que cuelga de {@code /api/admin}:
 * son dos rutas distintas porque devuelven cosas distintas.
 *
 * <p>Dueña: BE3 · Tarea: B2-07.
 */
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityCatalogController {

    private final ActivityCatalogService activityCatalogService;

    /**
     * Una página del catálogo, con los cuatro filtros opcionales.
     *
     * <p>El orden por defecto —fecha de inicio, desempatando por identificador—
     * no es un adorno: sin orden estable, al pasar de página la misma actividad
     * puede salir dos veces mientras otra no sale nunca.
     */
    @GetMapping
    public Page<ActivityCardResponse> list(
            @RequestParam(required = false) String line,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(sort = {"startDate", "id"}, direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication) {

        CatalogFilters filters = new CatalogFilters(line, mode, from, to);

        return activityCatalogService.list(filters, pageable, authentication.getName());
    }

    /** La ficha. En un estado no visible responde 404. */
    @GetMapping("/{id}")
    public ActivityDetailResponse detail(
            @PathVariable Long id,
            Authentication authentication) {

        return activityCatalogService.detail(id, authentication.getName());
    }
}
