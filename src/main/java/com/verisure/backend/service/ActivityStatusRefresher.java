package com.verisure.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.verisure.backend.dto.activity.RefreshStatusResponse;

import lombok.RequiredArgsConstructor;

/**
 * Encadena las dos transiciones y manda los avisos.
 *
 * <p><b>No lleva {@code @Transactional} a propósito:</b> orquesta, y la
 * transacción la abre el servicio al que llama. Así el correo de cada actividad
 * sale con su transacción ya confirmada.
 *
 * <p>Existe como clase aparte porque tiene dos disparadores —la tarea programada
 * y el endpoint de administración— y la secuencia no puede estar duplicada.
 *
 * <p>Dueña: BE3 · Tarea: B3-17.
 */
@Service
@RequiredArgsConstructor
public class ActivityStatusRefresher {

    private final ActivityStatusService activityStatusService;
    private final NotificationService notificationService;

    /** Empezar va antes que terminar: una actividad de un día hace las dos en la misma pasada. */
    public RefreshStatusResponse refresh() {
        int started = activityStatusService.startDueActivities();
        List<Long> finished = activityStatusService.finishDueActivities();

        for (Long activityId : finished) {
            notificationService.notifyActivityFinished(activityId);
        }

        return new RefreshStatusResponse(started, finished.size());
    }

}
