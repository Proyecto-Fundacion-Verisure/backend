package com.verisure.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.verisure.backend.dto.closure.CertificateResponse;
import com.verisure.backend.dto.closure.ClosureDetailResponse;
import com.verisure.backend.dto.closure.CreateClosureRequest;
import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.exception.UnsupportedMediaTypeException;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipationClosureServiceImpl implements ParticipationClosureService {

    /** El contrato admite PDF, JPG y PNG (docs/api-contract.md). El 413 lo traduce GlobalExceptionHandler. */
    private static final long MAX_EVIDENCE_BYTES = 10L * 1024 * 1024;
    private static final List<MediaType> SUPPORTED_EVIDENCE_TYPES =
            List.of(MediaType.APPLICATION_PDF, MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);

    private final ParticipationClosureRepository participationClosureRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ClosureDetailResponse submit(CreateClosureRequest request, MultipartFile evidence) {
        Registration registration = findRegistrationOrFail(request.registrationId());
        ensureConfirmed(registration);
        ensureActivityFinished(registration);
        ensureNotAlreadyClosed(request.registrationId());

        String evidenceUrl = storeEvidence(evidence, request.evidenceConsent());

        ParticipationClosure closure = new ParticipationClosure();
        closure.setActualHours(request.actualHours());
        closure.setRating(request.rating());
        closure.setComment(request.comment());
        closure.setEvidenceUrl(evidenceUrl);
        closure.setSubmittedAt(Instant.now());
        closure.setRegistration(registration);

        return ClosureDetailResponse.from(participationClosureRepository.save(closure));
    }

    @Override
    @Transactional(readOnly = true)
    public ClosureDetailResponse getById(Long closureId, String userEmail) {
        ParticipationClosure closure = findClosureOrFail(closureId);
        User requester = findUserOrFail(userEmail);
        ensureOwnerOrAdmin(closure, requester);

        return ClosureDetailResponse.from(closure);
    }

    @Override
    @Transactional(readOnly = true)
    public CertificateResponse getCertificate(Long closureId, String userEmail) {
        return null; // TODO B1-06
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

    /** Solo la persona propietaria o la administración pueden ver el cierre. */
    private void ensureOwnerOrAdmin(ParticipationClosure closure, User requester) {
        Registration registration = closure.getRegistration();
        User owner = registration.getUser();

        boolean isAdmin = requester.getRole() == Role.ADMIN;
        boolean isOwner = owner.getId().equals(requester.getId());
        if (!isAdmin && !isOwner) {
            throw new DomainException(ErrorCode.NOT_OWNER);
        }
    }

    private Registration findRegistrationOrFail(Long registrationId) {
        return registrationRepository.findById(registrationId)
                .orElseThrow(() -> NotFoundException.of("Registration", registrationId));
    }

    /**
     * «Confirmada» a efectos de cierre es CONFIRMED o PENDING_CLOSURE: la tarea
     * programada B3-17 ya ha movido la inscripción cuando la actividad llega a
     * FINISHED, así que un check estricto rompería el flujo feliz.
     */
    private void ensureConfirmed(Registration registration) {
        RegistrationStatus status = registration.getStatus();
        if (status != RegistrationStatus.CONFIRMED && status != RegistrationStatus.PENDING_CLOSURE) {
            throw new DomainException(ErrorCode.REGISTRATION_NOT_CONFIRMED);
        }
    }

    private void ensureActivityFinished(Registration registration) {
        if (registration.getActivity().getStatus() != ActivityStatus.FINISHED) {
            throw new DomainException(ErrorCode.ACTIVITY_NOT_FINISHED);
        }
    }

    private void ensureNotAlreadyClosed(Long registrationId) {
        if (participationClosureRepository.findByRegistrationId(registrationId).isPresent()) {
            throw new DomainException(ErrorCode.CLOSURE_ALREADY_CLOSED);
        }
    }

    /**
     * Valida la evidencia si viene; guardarla es de BE2. Sin archivo devuelve {@code null}.
     *
     * <p>Consent → 400, formato → 415, tamaño → 413: los traduce
     * {@link com.verisure.backend.exception.GlobalExceptionHandler} con la forma
     * única {@code ApiError}.
     */
    private String storeEvidence(MultipartFile evidence, Boolean evidenceConsent) {
        if (evidence == null || evidence.isEmpty()) {
            return null;
        }
        if (!Boolean.TRUE.equals(evidenceConsent)) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR,
                    "Para adjuntar evidencia, evidenceConsent debe ser true");
        }
        if (evidence.getSize() > MAX_EVIDENCE_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_EVIDENCE_BYTES);
        }

        MediaType type = parseContentType(evidence);
        if (type == null || !isSupported(type)) {
            throw new UnsupportedMediaTypeException("Tipo de archivo no admitido");
        }

        // TODO B1-03 · BE2: guardar la evidencia con FileStorageService (mismo patrón
        // que POST /api/admin/activity-images) y devolver su URL. Hasta que exista el
        // método, evidenceUrl queda null → afecta a evidenceCount (B1-04) y al certificado (B1-06).
        return null;
    }

    private boolean isSupported(MediaType type) {
        return type != null &&
                SUPPORTED_EVIDENCE_TYPES.stream().anyMatch(supported -> supported.isCompatibleWith(type));
    }

    /** JPG/PNG/PDF son tipos seguros del resolver multipart; un tipo malformado no puede ser soportado. */
    private MediaType parseContentType(MultipartFile evidence) {
        try {
            return MediaType.parseMediaType(evidence.getContentType());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
