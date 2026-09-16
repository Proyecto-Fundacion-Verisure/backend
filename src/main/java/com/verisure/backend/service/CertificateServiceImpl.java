package com.verisure.backend.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.closure.CertificateResponse;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.ActivityClosure;
import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityClosureStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ActivityClosureRepository;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Tercero de los tres servicios de cierre de BE1 · B1-06.
 *
 * <p>Solo la propietaria puede verlo. La verificación de propiedad y de
 * «actividad cerrada» vive aquí, no en el DTO.
 */
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final ParticipationClosureRepository participationClosureRepository;
    private final UserRepository userRepository;
    private final ActivityClosureRepository activityClosureRepository;

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse generate(Long closureId, String userEmail) {
        ParticipationClosure closure = findClosureOrFail(closureId);
        User requester = findUserOrFail(userEmail);

        ensureOwner(closure, requester);
        ensureActivityClosed(closure);

        return CertificateResponse.from(closure, Instant.now());
    }

    private ParticipationClosure findClosureOrFail(Long closureId) {
        return participationClosureRepository.findById(closureId)
                .orElseThrow(() -> NotFoundException.of("ParticipationClosure", closureId));
    }

    /** El correo es el identificador de la sesión: si el token es válido, la cuenta existe. */
    private User findUserOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
    }

    /**
     * El certificado es un dato personal: solo la propietaria. A diferencia del
     * detalle del cierre, aquí no entra administración.
     */
    private void ensureOwner(ParticipationClosure closure, User requester) {
        Registration registration = closure.getRegistration();
        User owner = registration.getUser();

        boolean isOwner = owner.getId().equals(requester.getId());
        if (!isOwner) {
            throw new DomainException(ErrorCode.NOT_OWNER,
                    "No puedes consultar este certificado");
        }
    }

    /**
     * El certificado se emite por cierre validado y solo tras el cierre de la
     * actividad completa: sin {@code ActivityClosure} en {@code CLOSED}, 409.
     */
    private void ensureActivityClosed(ParticipationClosure closure) {
        Activity activity = closure.getRegistration().getActivity();
        ActivityClosure activityClosure = activityClosureRepository
                .findByActivityId(activity.getId()).orElse(null);

        if (activityClosure == null || activityClosure.getStatus() != ActivityClosureStatus.CLOSED) {
            throw new DomainException(ErrorCode.ACTIVITY_NOT_CLOSED,
                    "El certificado todavía no está disponible");
        }
    }
}