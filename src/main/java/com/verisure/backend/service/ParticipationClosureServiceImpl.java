package com.verisure.backend.service;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
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
    private final FileStorageService fileStorageService;

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

        try {
            return ClosureDetailResponse.from(participationClosureRepository.saveAndFlush(closure));
        } catch (DataIntegrityViolationException e) {
            // Salvavidas para una carrera: dos POST simultáneos pasan
            // ensureNotAlreadyClosed y el segundo revienta el constraint único de
            // registration_id. El flush fuerza la violación dentro de este catch.
            throw new DomainException(ErrorCode.CLOSURE_ALREADY_CLOSED);
        }
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
     * Valida la evidencia si viene y la guarda. Sin archivo devuelve {@code null}.
     *
     * <p>Consent → 400, formato → 415, tamaño → 413: los traduce
     * {@link com.verisure.backend.exception.GlobalExceptionHandler} con la forma
     * única {@code ApiError}. El guardado es de BE2 ({@link FileStorageService}):
     * genera el nombre en el servidor (nunca el del cliente), deduce la extensión
     * del tipo de contenido y deja el archivo en {@code uploads/evidencias/}.
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

        return fileStorageService.store(evidence, "evidencias");
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
