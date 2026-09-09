package com.verisure.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.verisure.backend.dto.closure.CertificateResponse;
import com.verisure.backend.dto.closure.ClosureDetailResponse;
import com.verisure.backend.dto.closure.CreateClosureRequest;
import com.verisure.backend.service.ParticipationClosureService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Cierre de participación: lo rellena el empleado, uno por inscripción.
 *
 * <p><b>Este controlador NO inyecta {@link com.verisure.backend.service.NotificationService}.</b>
 * Este dominio no manda ningún correo. Si algún día hace falta, es señal de que
 * se ha colado un aviso donde no toca.
 *
 * <p>Las rutas cuelgan de {@code /api/closures/}, que la cadena de seguridad
 * ({@link com.verisure.backend.security.SpringConfig}) deja como
 * {@code anyRequest().authenticated()}. Por eso el rol se impone con
 * {@code @PreAuthorize} en cada método.
 *
 * <p>La propiedad del recurso ({@code NOT_OWNER}) se comprueba en el servicio,
 * no aquí: el controlador solo decide quién puede intentar, no si le toca.
 *
 * <p>Tarea: B1-03.
 */
@RestController
@RequestMapping("/api/closures")
@RequiredArgsConstructor
public class ParticipationClosureController {

    private final ParticipationClosureService participationClosureService;

    /**
     * El empleado envía su cierre: multipart con la parte {@code request} (JSON)
     * y la parte opcional {@code evidence} (PDF, JPG o PNG, máx 10 MB).
     *
     * <p>El servicio valida que la inscripción esté confirmada y la actividad
     * terminada antes de guardar.
     */
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ClosureDetailResponse> submit(
            @RequestPart("request") @Valid CreateClosureRequest request,
            @RequestPart(value = "evidence", required = false) MultipartFile evidence) {

        ClosureDetailResponse body = participationClosureService.submit(request, evidence);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Detalle de un cierre. El servicio comprueba que quien pide es el dueño o administración. */
    @GetMapping("/{id}")
    public ClosureDetailResponse getById(@PathVariable Long id) {
        return participationClosureService.getById(id);
    }

    /** Certificado de participación. Solo la persona propietaria. */
    @GetMapping("/{id}/certificate")
    public CertificateResponse getCertificate(@PathVariable Long id) {
        return participationClosureService.getCertificate(id);
    }
}
