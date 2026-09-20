package com.verisure.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;

/**
 * Las dos escrituras en masa sobre inscripciones que disparan otros dominios.
 *
 * <p>Los dos métodos usan la propagación por defecto y corren dentro de la
 * transacción de quien llama: o se cancela la actividad y sus inscripciones, o
 * no se cancela nada. Con {@code REQUIRES_NEW} podrían quedar las inscripciones
 * canceladas y la actividad viva.
 *
 * <p>Dueña: BE3 · Tarea: B3-06.
 */
@Service
@RequiredArgsConstructor
public class RegistrationLifecycleServiceImpl implements RegistrationLifecycleService {

    private final RegistrationRepository registrationRepository;

    /** No asciende a nadie: si se cancela la actividad, no hay plaza que dar. */
    @Override
    @Transactional
    public int cancelAllForActivity(Long activityId) {
        List<Registration> alive = registrationRepository.findByActivityIdAndStatusIn(
                activityId, List.of(RegistrationStatus.WAITLISTED, RegistrationStatus.CONFIRMED));

        for (Registration registration : alive) {
            registration.setStatus(RegistrationStatus.CANCELLED);
            registration.setQueuePosition(null);
        }
        return alive.size();
    }

    /** Es la transición que mete las horas en los dos dashboards. */
    @Override
    @Transactional
    public int closeAllForActivity(Long activityId) {
        List<Registration> pending = registrationRepository.findByActivityIdAndStatusIn(
                activityId, List.of(RegistrationStatus.PENDING_CLOSURE));

        for (Registration registration : pending) {
            registration.setStatus(RegistrationStatus.CLOSED);
        }
        return pending.size();
    }

}
