package com.verisure.backend.service;

import java.time.Instant;
import java.time.Year;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.RegistrationCounts;
import com.verisure.backend.repository.projection.RegistrationRow;

import lombok.RequiredArgsConstructor;

/**
 * Decisión de la administradora sobre una inscripción.
 *
 * <p>No toca {@code NotificationService}: el aviso lo manda el controlador
 * cuando estos métodos vuelven sin lanzar.
 *
 * <p>Dueña: BE3 · Tarea: B3-03.
 */
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final SpotService spotService;

    @Override
    @Transactional
    public Registration accept(Long registrationId, String adminEmail) {
        Registration registration = findRegistrationOrFail(registrationId);
        User admin = findAdminOrFail(adminEmail);

        registration.setAccepted(true);
        registration.setDecidedBy(admin);
        registration.setDecidedAt(Instant.now());

        if (spotService.hasFreeSpot(registration.getActivity().getId())) {
            registration.setStatus(RegistrationStatus.CONFIRMED);
            registration.setQueuePosition(null);
            spotService.refreshFullStatus(registration.getActivity().getId());
        }
        // sin hueco: se queda WAITLISTED con accepted = true, en su posición

        return registration;
    }

    @Override
    @Transactional
    public Registration reject(Long registrationId, String adminEmail) {
        Registration registration = findRegistrationOrFail(registrationId);
        User admin = findAdminOrFail(adminEmail);

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setDecidedBy(admin);
        registration.setDecidedAt(Instant.now());

        return registration;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationRow> getAdminDashboard(Long activityId,
                                                   RegistrationStatus status,
                                                   Pageable pageable) {
        int currentYear = Year.now().getValue();

        return registrationRepository.findAdminDashboard(activityId, status, currentYear, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistrationCounts getCounts(Long activityId) {
        return registrationRepository.findCountsByActivityId(activityId);
    }

    private Registration findRegistrationOrFail(Long registrationId) {
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> NotFoundException.of("inscripción", registrationId));
    }

    /** Se carga aquí y no llega desde el token para que quede gestionada por Hibernate. */
    private User findAdminOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
    }
}
