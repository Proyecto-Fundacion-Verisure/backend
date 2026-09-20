package com.verisure.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.activity.ActivityFormResponse;
import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.activity.ReturnActivityRequest;
import com.verisure.backend.dto.activity.UpdateActivityRequest;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.repository.projection.ActivitySummary;
import com.verisure.backend.service.ActivityService;
import com.verisure.backend.service.NotificationService;

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
    private final NotificationService notificationService;

    /**
     * El listado de actividades de administración, con filtro de estado opcional.
     *
     * <p>Ordenado por fecha de inicio descendente: lo que se viene a tocar aquí
     * es lo próximo, no lo del año pasado. La cola de revisión va al revés, por
     * antigüedad, porque eso sí es una cola.
     */
    @GetMapping("/activities")
    public Page<ActivitySummary> list(
            @RequestParam(required = false) ActivityStatus status,
            @PageableDefault(sort = "startDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return activityService.list(status, pageable);
    }

    /** Crea la actividad en DRAFT. Devuelve 201. */
    @PostMapping("/activities")
    public ResponseEntity<ActivityResponse> create(
            @Valid @RequestBody CreateActivityRequest request,
            Authentication authentication) {
        ActivityResponse body = activityService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Carga el formulario de edición, en cualquier estado · B2-05. */
    @GetMapping("/activities/{id}")
    public ActivityFormResponse getForm(@PathVariable Long id) {
        return activityService.getForm(id);
    }

    /** Actualiza la actividad, con las mismas validaciones que la creación · B2-05. */
    @PutMapping("/activities/{id}")
    public ActivityResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateActivityRequest request) {
        return activityService.update(id, request);
    }

    /**
     * Cancela la actividad y sus inscripciones · B2-05.
     *
     * <p>Orquesta el aviso, como el resto de controladores: el servicio hace su
     * trabajo dentro de su transacción y devuelve; si no ha lanzado, el
     * controlador manda el correo. Así una cancelación que se deshace no avisa
     * a nadie. Ver la regla en {@link NotificationService}.
     */
    @PatchMapping("/activities/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        activityService.cancel(id);
        notificationService.notifyActivityCancelled(id);
        return ResponseEntity.noContent().build();
    }

    /** Publica la actividad: DRAFT → PUBLISHED. */
    @PatchMapping("/activities/{id}/publish")
    public ActivityResponse publish(@PathVariable Long id) {
        return activityService.publish(id);
    }

    /**
     * La cola de actividades propuestas pendientes de revisión · {@code B2-15},
     * por antigüedad (la de fecha de inicio más antigua primero).
     */
    @GetMapping("/activities/pending")
    public Page<ActivitySummary> pending(
            @PageableDefault(sort = "startDate", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return activityService.listPendingApproval(pageable);
    }

    /**
     * Aprueba una actividad propuesta y la publica · {@code B2-15}.
     *
     * <p>Da 409 si la actividad no está en {@code PENDING_APPROVAL}. Orquesta el
     * aviso fuera de la transacción, como el resto de controladores.
     */
    @PatchMapping("/activities/{id}/approve")
    public ActivityResponse approve(@PathVariable Long id) {
        ActivityResponse approved = activityService.approve(id);
        notificationService.notifyActivityApproved(id);
        return approved;
    }

    /**
     * Devuelve una actividad propuesta a su entidad con un comentario · {@code B2-15}.
     *
     * <p>El comentario es obligatorio: sin él, 400. Da 409 si la actividad no
     * está en {@code PENDING_APPROVAL}. Orquesta el aviso fuera de la transacción.
     */
    @PatchMapping("/activities/{id}/return")
    public ActivityResponse returnToDraft(
            @PathVariable Long id,
            @Valid @RequestBody ReturnActivityRequest request) {
        ActivityResponse returned = activityService.returnToDraft(id, request.note());
        notificationService.notifyActivityReturned(id);
        return returned;
    }
}