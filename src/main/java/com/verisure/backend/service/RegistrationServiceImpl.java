package com.verisure.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.registration.CancelResult;
import com.verisure.backend.dto.registration.MyRegistrationItem;
import com.verisure.backend.dto.registration.RegistrationResponse;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.RegistrationCounts;
import com.verisure.backend.repository.projection.RegistrationRow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Decisión de la administradora sobre una inscripción, y baja desde los dos
 * lados.
 *
 * <p>No toca {@code NotificationService}: el aviso lo manda el controlador
 * cuando estos métodos vuelven sin lanzar.
 *
 * <p>Dueña: BE3 · Tarea: B3-03 · B3-06.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final SpotService spotService;

    @Override
    @Transactional
    public RegistrationResponse accept(Long registrationId, String adminEmail) {
        Registration registration = findRegistrationOrFail(registrationId);
        User admin = findUserOrFail(adminEmail);

        registration.setAccepted(true);
        registration.setDecidedBy(admin);
        registration.setDecidedAt(Instant.now());

        Long activityId = registration.getActivity().getId();
        if (spotService.hasFreeSpot(activityId)) {
            registration.setStatus(RegistrationStatus.CONFIRMED);
            registration.setQueuePosition(null);
            spotService.reorderQueue(activityId);
            spotService.refreshFullStatus(activityId);
        }
        // sin hueco: se queda WAITLISTED con accepted = true, en su posición

        return RegistrationResponse.from(registration);
    }

    @Override
    @Transactional
    public RegistrationResponse reject(Long registrationId, String adminEmail) {
        Registration registration = findRegistrationOrFail(registrationId);
        User admin = findUserOrFail(adminEmail);

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setQueuePosition(null);
        registration.setDecidedBy(admin);
        registration.setDecidedAt(Instant.now());

        Long activityId = registration.getActivity().getId();
        spotService.reorderQueue(activityId);

        return RegistrationResponse.from(registration);
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
        return registrationRepository.findCounts(activityId);
    }

    @Override
    @Transactional
    public CancelResult cancel(Long registrationId, String callerEmail, String reason) {
        Registration registration = findRegistrationOrFail(registrationId);
        User caller = findUserOrFail(callerEmail);

        boolean isAdmin = caller.getRole() == Role.ADMIN;
        boolean isOwner = registration.getUser().getId().equals(caller.getId());

        // Primero quién eres y después el plazo: al revés, a un tercero se le
        // diría «llegas tarde» sobre una inscripción que no es suya.
        if (!isAdmin && !isOwner) {
            throw new DomainException(ErrorCode.NOT_OWNER);
        }

        boolean activityAlreadyStarted = LocalDate.now()
                .isAfter(registration.getActivity().getStartDate());
        if (!isAdmin && activityAlreadyStarted) {
            throw new DomainException(ErrorCode.DEADLINE_PASSED);
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setQueuePosition(null);

        log.info("Inscripción {} cancelada por {} · motivo: {}", registrationId, callerEmail, reason);

        Long activityId = registration.getActivity().getId();
        Long promotedRegistrationId = spotService.promoteFirstInQueue(activityId);

        // Fuera del ascenso también: promoteFirstInQueue solo sincroniza cuando
        // hay a quién ascender, y con la cola vacía la actividad se quedaría en
        // FULL teniendo plazas libres.
        spotService.refreshFullStatus(activityId);
        spotService.reorderQueue(activityId);

        return new CancelResult(RegistrationResponse.from(registration), promotedRegistrationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyRegistrationItem> findMine(String userEmail) {
        User user = findUserOrFail(userEmail);

        return registrationRepository.findMine(user.getId());
    }

    /** Trae la actividad cargada: la respuesta lleva su título. */
    private Registration findRegistrationOrFail(Long registrationId) {
        return registrationRepository.findByIdWithActivity(registrationId)
                .orElseThrow(() -> NotFoundException.of("inscripción", registrationId));
    }

    /** Se carga aquí y no llega desde el token para que quede gestionada por Hibernate. */
    private User findUserOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
    }
}
