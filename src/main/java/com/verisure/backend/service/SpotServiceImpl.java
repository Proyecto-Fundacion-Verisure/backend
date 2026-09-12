package com.verisure.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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

    @Override
    @Transactional
    public Long promoteFirstInQueue(Long activityId) {
        lockActivityOrFail(activityId);

        boolean noSpotAvailable = !hasFreeSpot(activityId);
        if (noSpotAvailable) {
            return null;
        }

        Registration promoted = findFirstAcceptedInQueue(activityId);
        if (promoted == null) {
            return null;
        }

        promoted.setStatus(RegistrationStatus.CONFIRMED);
        promoted.setQueuePosition(null);

        reorderQueue(activityId);
        refreshFullStatus(activityId);

        return promoted.getId();
    }

    /**
     * Bloquea la fila de la actividad hasta que confirme la transacción.
     *
     * <p>Va antes de leer el cupo: leerlo primero y bloquear después es haber
     * leído ya el dato viejo, y entonces el bloqueo no sirve de nada.
     */
    private void lockActivityOrFail(Long activityId) {
        activityRepository.findByIdForUpdate(activityId)
                .orElseThrow(() -> NotFoundException.of("actividad", activityId));
    }

    /**
     * La primera de la cola que además esté aceptada, o null si no hay ninguna.
     *
     * <p>Estar la primera no basta: quien no ha pasado por la decisión de la
     * administradora no se cuela por delante de quien sí.
     */
    private Registration findFirstAcceptedInQueue(Long activityId) {
        List<Registration> queue = findQueue(activityId);

        for (Registration registration : queue) {
            if (registration.isAccepted()) {
                return registration;
            }
        }
        return null;
    }

    /** Renumera la cola a 1, 2, 3… sin huecos tras sacar a alguien de ella. */
    private void reorderQueue(Long activityId) {
        List<Registration> queue = findQueue(activityId);

        int position = 1;
        for (Registration registration : queue) {
            registration.setQueuePosition(position);
            position++;
        }
    }

    private List<Registration> findQueue(Long activityId) {
        return registrationRepository.findByActivityIdAndStatusOrderByQueuePosition(
                activityId, RegistrationStatus.WAITLISTED);
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
