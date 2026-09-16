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

import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.org.OrgActivityRow;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.service.NotificationService;
import com.verisure.backend.service.OrgActivityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Las actividades del rol entidad.
 *
 * <p>No lleva {@code @PreAuthorize}: la cadena de seguridad ya reserva
 * {@code /api/org/**} al rol {@code PARTNER}, así que una empleada o la
 * administradora reciben 403 sin llegar hasta aquí.
 *
 * <p><b>Ningún método recibe un identificador de entidad.</b> Lo resuelve el
 * servicio desde el token; ver {@code PartnerResolver}.
 *
 * <p>Dueña: BE3 · Tareas: B2-13 y B2-14.
 */
@RestController
@RequestMapping("/api/org/activities")
@RequiredArgsConstructor
public class OrgActivityController {

    private final OrgActivityService orgActivityService;
    private final NotificationService notificationService;

    /** Una página de las actividades de la entidad, lo más reciente delante. */
    @GetMapping
    public Page<OrgActivityRow> list(
            @RequestParam(required = false) ActivityStatus status,
            @PageableDefault(sort = {"startDate", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            Authentication authentication) {

        return orgActivityService.list(status, pageable, authentication.getName());
    }

    /** Crea una actividad en borrador. Responde 201 porque crea un recurso nuevo. */
    @PostMapping
    public ResponseEntity<OrgActivityRow> create(
            @Valid @RequestBody CreateActivityRequest request,
            Authentication authentication) {

        OrgActivityRow body = orgActivityService.create(request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Reescribe un borrador propio. En cualquier otro estado, 409. */
    @PutMapping("/{id}")
    public OrgActivityRow update(
            @PathVariable Long id,
            @Valid @RequestBody CreateActivityRequest request,
            Authentication authentication) {

        return orgActivityService.update(id, request, authentication.getName());
    }

    /**
     * Envía el borrador a revisión · {@code B2-14}.
     *
     * <p>El aviso sale aquí y no dentro del servicio para que el correo se mande
     * con la transacción ya confirmada.
     */
    @PatchMapping("/{id}/submit")
    public OrgActivityRow submit(@PathVariable Long id, Authentication authentication) {
        OrgActivityRow submitted = orgActivityService.submit(id, authentication.getName());

        notificationService.notifyActivitySubmittedForReview(id);

        return submitted;
    }
}
