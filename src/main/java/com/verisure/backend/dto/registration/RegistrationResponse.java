package com.verisure.backend.dto.registration;

import java.time.Instant;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;

/**
 * Una inscripción tal y como la ve quien la solicitó.
 *
 * <p>accepted y status son cosas distintas: accepted dice si la admin dio por
 * buena a la persona, status dice dónde está. Se puede estar aceptado y seguir
 * en WAITLISTED. queuePosition es nulo fuera de la cola.
 *
 * <p>from() lee la relación perezosa activity: llamarla dentro de la sesión.
 *
 * <p>Dueña: BE3 · Tarea: B3-02.
 */
public record RegistrationResponse(
        Long id,
        Long activityId,
        String activityTitle,
        RegistrationStatus status,
        boolean accepted,
        Integer queuePosition,
        Instant createdAt) {

    public static RegistrationResponse from(Registration registration) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getActivity().getId(),
                registration.getActivity().getTitle(),
                registration.getStatus(),
                registration.isAccepted(),
                registration.getQueuePosition(),
                registration.getCreatedAt());
    }
}
