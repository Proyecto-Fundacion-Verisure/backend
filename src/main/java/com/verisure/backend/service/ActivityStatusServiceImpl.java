package com.verisure.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Aplica las dos transiciones que dependen de las fechas.
 *
 * <p>Son idempotentes sin llevar ninguna marca: una actividad que ya está en el
 * estado siguiente deja de salir en la consulta, así que una segunda ejecución
 * el mismo día no encuentra nada que hacer.
 *
 * <p>Dueña: BE3 · Tarea: B3-17.
 */
@Service
@RequiredArgsConstructor
public class ActivityStatusServiceImpl implements ActivityStatusService {

    private static final List<ActivityStatus> OPEN_STATUSES =
            List.of(ActivityStatus.PUBLISHED, ActivityStatus.FULL);

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;

    @Override
    @Transactional
    public int startDueActivities() {
        List<Activity> due = activityRepository.findByStatusInAndStartDateLessThanEqual(
                OPEN_STATUSES, LocalDate.now());

        for (Activity activity : due) {
            activity.setStatus(ActivityStatus.IN_PROGRESS);
        }
        return due.size();
    }

    @Override
    @Transactional
    public List<Long> finishDueActivities() {
        List<Activity> due = activityRepository.findByStatusAndEndDateBefore(
                ActivityStatus.IN_PROGRESS, LocalDate.now());

        List<Long> finished = new ArrayList<>();
        for (Activity activity : due) {
            activity.setStatus(ActivityStatus.FINISHED);
            markConfirmedAsPendingClosure(activity.getId());
            cancelRemainingQueue(activity.getId());
            finished.add(activity.getId());
        }
        return finished;
    }

    /** La transición que habilita el cierre del empleado y el de la Fundación. */
    private void markConfirmedAsPendingClosure(Long activityId) {
        List<Registration> confirmed = registrationRepository.findByActivityIdAndStatusIn(
                activityId, List.of(RegistrationStatus.CONFIRMED));

        for (Registration registration : confirmed) {
            registration.setStatus(RegistrationStatus.PENDING_CLOSURE);
        }
    }

    /**
     * Cierra la cola de una actividad que ya ha terminado.
     *
     * <p>No las canceló nadie: la actividad acabó sin que les llegara a tocar
     * plaza. Se usa {@code CANCELLED} por ser el único estado terminal que
     * encaja, para no dejarlas en cola de algo que ya pasó.
     */
    private void cancelRemainingQueue(Long activityId) {
        List<Registration> queued = registrationRepository.findByActivityIdAndStatusIn(
                activityId, List.of(RegistrationStatus.WAITLISTED));

        for (Registration registration : queued) {
            registration.setStatus(RegistrationStatus.CANCELLED);
            registration.setQueuePosition(null);
        }
    }

}
