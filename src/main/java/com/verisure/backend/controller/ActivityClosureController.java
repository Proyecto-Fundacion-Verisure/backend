package com.verisure.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.activityclosure.ActivityClosureResponse;
import com.verisure.backend.dto.activityclosure.ActivityClosureRow;
import com.verisure.backend.dto.activityclosure.SaveActivityClosureRequest;
import com.verisure.backend.service.ActivityClosureService;
import com.verisure.backend.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Cierre de actividad. Solo administración.
 *
 * <p><b>Este controlador orquesta el aviso</b>, que es la razón por la que
 * inyecta {@link NotificationService}: el servicio hace su trabajo dentro de su
 * transacción y devuelve; si no ha lanzado, el controlador manda el correo. Así
 * una operación que se deshace no avisa a nadie. Ver la regla completa en
 * {@link NotificationService}.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityClosureController {

    private final ActivityClosureService activityClosureService;
    private final NotificationService notificationService;

    /** Cola de actividades finalizadas pendientes de cerrar. */
    @GetMapping("/admin/activities/pending-closure")
    public Page<ActivityClosureRow> findPendingClosure(Pageable pageable) {
        return activityClosureService.findPendingClosure(pageable);
    }

    /** Borrador del cierre más los agregados de participación. */
    @GetMapping("/activities/{id}/closure")
    public ActivityClosureResponse getByActivity(@PathVariable Long id) {
        return activityClosureService.getByActivity(id);
    }

    /** Guarda el borrador sin cerrar. */
    @PutMapping("/activities/{id}/closure")
    public ActivityClosureResponse saveDraft(@PathVariable Long id,
                                             @Valid @RequestBody SaveActivityClosureRequest request) {
        return activityClosureService.saveDraft(id, request);
    }

    /**
     * Cierra la actividad y avisa a los participantes. No se deshace.
     *
     * <p>Las dos líneas del cuerpo están en este orden a propósito: si el
     * servicio lanza, no se llega a la segunda y no sale ningún correo.
     */
    @PatchMapping("/activities/{id}/closure/finalize")
    public ResponseEntity<ActivityClosureResponse> finalizeClosure(@PathVariable Long id) {
        ActivityClosureResponse body = activityClosureService.finalizeClosure(id); // ya confirmó
        notificationService.notifyActivityClosed(id);
        return ResponseEntity.ok(body);
    }
}
