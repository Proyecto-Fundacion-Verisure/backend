package com.verisure.backend.service;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.SpotInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SpotServiceImpl implements SpotService {

    private final RegistrationRepository registrationRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Registration register(Long activityId, String userEmail) {
        SpotInfo spot = findSpotInfoOrFail(activityId);
        User user = findUserOrFail(userEmail);

        boolean hasLiveRegistration = registrationRepository.existsByActivityIdAndUserIdAndStatusNot(
                activityId, user.getId(), RegistrationStatus.CANCELLED);
        if (hasLiveRegistration) {
            throw new DomainException(ErrorCode.ALREADY_REGISTERED);
        }

        boolean deadlinePassed = LocalDate.now().isAfter(spot.registrationDeadline());
        if (deadlinePassed) {
            throw new DomainException(ErrorCode.DEADLINE_PASSED);
        }

        Registration registration = buildQueuedRegistration(activityId, user);
        return registrationRepository.save(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasFreeSpot(Long activityId) {
        SpotInfo spot = findSpotInfoOrFail(activityId);

        long confirmed = spot.confirmed();
        int totalSpots = spot.spots();
        return confirmed < totalSpots;
    }

    @Override
    @Transactional
    public void refreshFullStatus(Long activityId) {
        SpotInfo spot = findSpotInfoOrFail(activityId);
        Activity activity = activityRepository.getReferenceById(activityId);

        boolean allSpotsTaken = spot.confirmed() >= spot.spots();
        boolean isPublished = activity.getStatus() == ActivityStatus.PUBLISHED;
        boolean isFull = activity.getStatus() == ActivityStatus.FULL;

        if (allSpotsTaken && isPublished) {
            activity.setStatus(ActivityStatus.FULL);
        }
        if (!allSpotsTaken && isFull) {
            activity.setStatus(ActivityStatus.PUBLISHED);
        }
    }

    /** Cupo, confirmadas y fecha límite en una consulta, sin cargar Activity. */
    private SpotInfo findSpotInfoOrFail(Long activityId) {
        return activityRepository.findSpotInfo(activityId)
                .orElseThrow(() -> NotFoundException.of("actividad", activityId));
    }

    private User findUserOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
    }

    /**
     * Arma la inscripción nueva, al final de la cola y sin decidir.
     *
     * <p>createdAt se pone a mano: la entidad no tiene @PrePersist.
     *
     * <p>La actividad se carga entera y no con {@code getReferenceById}, porque
     * el controlador lee su título al construir la respuesta, ya fuera de la
     * transacción: un proxy sin inicializar revienta ahí.
     */
    private Registration buildQueuedRegistration(Long activityId, User user) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> NotFoundException.of("actividad", activityId));

        Registration registration = new Registration();
        registration.setActivity(activity);
        registration.setUser(user);
        registration.setStatus(RegistrationStatus.WAITLISTED);
        registration.setAccepted(false);
        registration.setQueuePosition(nextQueuePosition(activityId));
        registration.setCreatedAt(Instant.now());
        return registration;
    }

    private Integer nextQueuePosition(Long activityId) {
        long queued = registrationRepository.countByActivityIdAndStatus(
                activityId, RegistrationStatus.WAITLISTED);
        return (int) (queued + 1);
    }

}
