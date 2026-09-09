package com.verisure.backend.dto.closure;

import java.time.LocalDate;

/**
 * Respuesta de {@code GET /api/closures/{id}/certificate}.
 *
 * <p>Se construye cruzando la cadena
 * {@code ParticipationClosure → Registration → Activity → Partner}
 * y {@code Registration → User}. La lógica de negocio (comprobar que la
 * actividad esté cerrada y que quien pide es la propietaria) vive en
 * {@code CertificateService}, no aquí.
 *
 * <p>Tarea: B1-03 (DTO) · B1-06 (lógica del certificado).
 */
public record CertificateResponse(
        Long closureId,
        String employeeName,
        String activityTitle,
        String partnerName,
        Integer actualHours,
        LocalDate activityEndDate,
        String department,
        String organization) {
}
