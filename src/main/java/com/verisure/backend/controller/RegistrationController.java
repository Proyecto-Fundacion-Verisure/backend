package com.verisure.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.registration.CreateRegistrationRequest;
import com.verisure.backend.dto.registration.RegistrationResponse;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.projection.RegistrationCounts;
import com.verisure.backend.repository.projection.RegistrationRow;
import com.verisure.backend.service.NotificationService;
import com.verisure.backend.service.RegistrationService;
import com.verisure.backend.service.SpotService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Inscripciones: solicitud, decisión y tablero de administración.
 *
 * <p>Inyecta {@link NotificationService} porque el aviso se manda aquí, después
 * de que el servicio haya vuelto sin lanzar: así una operación que se deshace no
 * avisa a nadie.
 *
 * <p>Las rutas de decisión cuelgan de {@code /api/registrations/} por contrato,
 * donde la cadena solo exige token, así que <b>el rol lo impone su
 * {@code @PreAuthorize}</b>: sin él un empleado aceptaría su propia inscripción.
 *
 * <p>Dueña: BE3 · Tarea: B3-03.
 */
@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final SpotService spotService;
    private final RegistrationService registrationService;
    private final NotificationService notificationService;

    /**
     * Solicita plaza: crea la inscripción en {@code WAITLISTED}.
     *
     * <p>Que la actividad esté llena o vacía no importa: la decisión es de la
     * administradora.
     */
    @PostMapping("/api/registrations")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody CreateRegistrationRequest request,
            Authentication authentication) {

        Registration registration = spotService.register(
                request.activityId(), authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(RegistrationResponse.from(registration));
    }

    /** Tablero de administración: inscripciones con las horas del año de cada persona. */
    @GetMapping("/api/admin/registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<RegistrationRow> getAdminDashboard(
            @RequestParam(required = false) Long activityId,
            @RequestParam(required = false) RegistrationStatus status,
            Pageable pageable) {

        return registrationService.getAdminDashboard(activityId, status, pageable);
    }

    /**
     * Contadores del tablero: confirmadas, en cola y sin revisar.
     *
     * <p>Van en su propia ruta porque el contrato fija la del tablero como
     * {@code Page<RegistrationRow>}, y ahí no caben.
     */
    @GetMapping("/api/admin/registrations/counts")
    @PreAuthorize("hasRole('ADMIN')")
    public RegistrationCounts getCounts(@RequestParam Long activityId) {
        return registrationService.getCounts(activityId);
    }

    /**
     * Acepta una inscripción: la marca como apta.
     *
     * <p>Si hay hueco, la confirma; si no, se queda en cola con
     * {@code accepted = true}. <b>Nunca falla por aforo.</b>
     *
     * <p>El aviso sale después de que el servicio ha vuelto sin lanzar. Si
     * el servicio lanza, no se llega a la línea del correo.
     */
    @PatchMapping("/api/registrations/{id}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public RegistrationResponse accept(
            @PathVariable Long id,
            Authentication authentication) {

        Registration registration = registrationService.accept(id, authentication.getName());

        if (registration.getStatus() == RegistrationStatus.CONFIRMED) {
            notificationService.notifyRegistrationConfirmed(id);
        } else {
            notificationService.notifyRegistrationWaitlisted(id);
        }

        return RegistrationResponse.from(registration);
    }

    /**
     * Rechaza una inscripción.
     *
     * <p>Sin cuerpo y sin motivo. El rechazo es un estado, no una
     * conversación.
     */
    @PatchMapping("/api/registrations/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public RegistrationResponse reject(
            @PathVariable Long id,
            Authentication authentication) {

        Registration registration = registrationService.reject(id, authentication.getName());

        notificationService.notifyRegistrationRejected(id);

        return RegistrationResponse.from(registration);
    }
}
