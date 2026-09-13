package com.verisure.backend.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.activity.RefreshStatusResponse;
import com.verisure.backend.service.ActivityStatusRefresher;

import lombok.RequiredArgsConstructor;

/**
 * Disparo manual del paso de estados de las actividades.
 *
 * <p>Existe para no depender de esperar a las tres de la madrugada al enseñar
 * la demo. Hace exactamente lo mismo que la tarea programada.
 *
 * <p><b>Este endpoint no está en el contrato de API</b>: es la enmienda
 * pendiente número 1, redactada en {@code docs/b3-17-enmienda-contrato.md}.
 *
 * <p>Dueña: BE3 · Tarea: B3-17.
 */
@RestController
@RequiredArgsConstructor
public class ActivityStatusController {

    private final ActivityStatusRefresher activityStatusRefresher;

    @PostMapping("/api/admin/activities/refresh-status")
    @PreAuthorize("hasRole('ADMIN')")
    public RefreshStatusResponse refreshStatus() {
        return activityStatusRefresher.refresh();
    }

}
