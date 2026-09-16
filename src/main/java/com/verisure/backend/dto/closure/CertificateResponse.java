package com.verisure.backend.dto.closure;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;

/**
 * Respuesta de {@code GET /api/closures/{id}/certificate}.
 *
 * <p>Se construye cruzando la cadena
 * {@code ParticipationClosure → Registration → Activity → Partner}
 * y {@code Registration → User}. {@code partnerName} es {@code null} si la
 * actividad no tiene entidad colaboradora. La lógica de negocio (actividad
 * cerrada y propiedad) vive en {@code CertificateService}, no aquí.
 *
 * <p>{@code from()} lee relaciones perezosas: llamarla dentro de la transacción
 * del servicio ({@code open-in-view=false}). {@code issuedAt} lo decide quien
 * llama, para que los tests lo controlen.
 *
 * <p>Tarea: B1-06.
 */
public record CertificateResponse(
        Long closureId,
        String fullName,
        String activityTitle,
        String partnerName,
        String line,
        OffsetDateTime startDate,
        OffsetDateTime endDate,
        Integer actualHours,
        Instant issuedAt,
        String reference) {

    public static CertificateResponse from(ParticipationClosure closure, Instant issuedAt) {
        Registration registration = closure.getRegistration();
        Activity activity = registration.getActivity();
        User user = registration.getUser();
        Partner partner = activity.getPartner();
        return new CertificateResponse(
                closure.getId(),
                user.getFullName(),
                activity.getTitle(),
                partner == null ? null : partner.getName(),
                activity.getLine(),
                activity.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC),
                activity.getEndDate().atStartOfDay().atOffset(ZoneOffset.UTC),
                closure.getActualHours(),
                issuedAt,
                closure.getReference());
    }
}