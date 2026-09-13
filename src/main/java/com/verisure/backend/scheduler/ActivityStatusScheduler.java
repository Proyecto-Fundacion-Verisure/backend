package com.verisure.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.verisure.backend.dto.activity.RefreshStatusResponse;
import com.verisure.backend.service.ActivityStatusRefresher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Dispara el paso de estados de las actividades una vez al día.
 *
 * <p>Dueña: BE3 · Tarea: B3-17.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final ActivityStatusRefresher activityStatusRefresher;

    /** De madrugada, cuando no hay nadie usando la plataforma. */
    @Scheduled(cron = "0 0 3 * * *")
    public void refreshStatuses() {
        RefreshStatusResponse result = activityStatusRefresher.refresh();

        log.info("Paso de estados: {} empezadas, {} terminadas",
                result.started(), result.finished());
    }

}
