package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import com.verisure.backend.dto.closure.CertificateResponse;
import com.verisure.backend.dto.closure.ClosureDetailResponse;
import com.verisure.backend.dto.closure.CreateClosureRequest;
import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ParticipationClosureServiceImpl implements ParticipationClosureService {

    private final ParticipationClosureRepository participationClosureRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ClosureDetailResponse submit(CreateClosureRequest request, MultipartFile evidence) {
        return null; // TODO B1-03
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
}
