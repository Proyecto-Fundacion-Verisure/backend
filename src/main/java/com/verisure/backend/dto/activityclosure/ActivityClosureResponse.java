package com.verisure.backend.dto.activityclosure;

import java.time.Instant;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.ActivityClosure;
import com.verisure.backend.entity.enums.ActivityClosureStatus;
import com.verisure.backend.repository.projection.ActivityClosureAggregates;

/**
 * Detalle de cierre de actividad para {@code GET/PUT/PATCH
 * /api/admin/activities/{id}/closure}.
 *
 * <p>Si la actividad no tiene cierre ({@code cierre == null}), los campos del
 * cierre van a {@code null} y {@code status} es {@code DRAFT}: los agregados y
 * las horas previstas siempre se calculan.
 *
 * <p>{@code from()} lee la relación perezosa {@code activity}: llamarla dentro
 * de la transacción (patrón de {@code ClosureDetailResponse.from}).
 *
 * <p>{@code evidenceCount} sale de {@code participation_closures.evidence_url}
 * y será 0 hasta que BE2 entregue {@code FileStorageService} (asumido en
 * b1-orden-plan.md).
 *
 * <p>Tarea: B1-04.
 */
public record ActivityClosureResponse(
        Long activityId,
        Integer collaborationRating,
        String closingNotes,
        String lessonsLearned,
        ActivityClosureStatus status,
        Instant closedAt,
        Integer expectedHours,
        Long reportedHours,
        long confirmedVolunteers,
        long closedParticipations,
        long evidenceCount) {

    /**
     * Construye el detalle del cierre de una actividad.
     *
     * @param cierreNullable     cierre de la actividad, o {@code null} si aún no
     *                           existe
     * @param activity           actividad; {@code hours} nunca es {@code null}
     * @param agg                agregados sobre los cierres de participación
     * @param confirmedVolunteers voluntarios {@code CONFIRMED} de la actividad
     */
    public static ActivityClosureResponse from(ActivityClosure cierreNullable,
            Activity activity, ActivityClosureAggregates agg, long confirmedVolunteers) {
        boolean sinCierre = cierreNullable == null;
        return new ActivityClosureResponse(
                activity.getId(),
                sinCierre ? null : cierreNullable.getCollaborationRating(),
                sinCierre ? null : cierreNullable.getClosingNotes(),
                sinCierre ? null : cierreNullable.getLessonsLearned(),
                sinCierre ? ActivityClosureStatus.DRAFT : cierreNullable.getStatus(),
                sinCierre ? null : cierreNullable.getClosedAt(),
                Math.toIntExact(activity.getHours() * confirmedVolunteers),
                agg.reportedHours(),
                confirmedVolunteers,
                agg.closedParticipations(),
                agg.evidenceCount());
    }
}
