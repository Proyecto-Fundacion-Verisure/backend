package com.verisure.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.service.ActivityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Administración de actividades. Solo administración.
 *
 * <p>El creador se resuelve desde la sesión (el correo es el nombre del contexto
 * de seguridad), nunca desde el cuerpo, y ninguna entidad sale por aquí.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    /** Crea la actividad en DRAFT. Devuelve 201. */
    @PostMapping("/activities")
    public ResponseEntity<ActivityResponse> create(
            @Valid @RequestBody CreateActivityRequest request,
            Authentication authentication) {
        ActivityResponse body = activityService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Publica la actividad: DRAFT → PUBLISHED. */
    @PatchMapping("/activities/{id}/publish")
    public ActivityResponse publish(@PathVariable Long id) {
        return activityService.publish(id);
    }
}