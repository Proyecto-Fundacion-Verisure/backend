package com.verisure.backend.dto.closure;

import java.time.Instant;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.entity.Registration;

/**
 * Detalle de un cierre de participación. Accesible para administración o para
 * la persona propietaria.
 *
 * <p>No hay estados: la fila existe o no existe. Las horas que declara el
 * empleado son las definitivas ({@code actualHours}).
 *
 * <p>{@code from()} lee las relaciones perezosas {@code registration} y
 * {@code activity}: llamarla dentro de la sesión.
 *
 * <p>{@code evidenceUrl} es absoluta y apunta al backend, lista para un
 * {@code href} o un {@code src}; la compone el servicio, porque el record no
 * sabe desde qué host se le pide.
 *
 * <p>Tarea: B1-03.
 */
public record ClosureDetailResponse(
        Long id,
        Long registrationId,
        Long activityId,
        String activityTitle,
        Integer actualHours,
        Integer rating,
        String comment,
        String evidenceUrl,
        Instant submittedAt) {

    public static ClosureDetailResponse from(ParticipationClosure closure, String evidenceUrl) {
        Registration registration = closure.getRegistration();
        Activity activity = registration.getActivity();
        return new ClosureDetailResponse(
                closure.getId(),
                registration.getId(),
                activity.getId(),
                activity.getTitle(),
                closure.getActualHours(),
                closure.getRating(),
                closure.getComment(),
                evidenceUrl,
                closure.getSubmittedAt());
    }
}
