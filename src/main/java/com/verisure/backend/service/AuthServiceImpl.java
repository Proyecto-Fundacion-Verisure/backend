package com.verisure.backend.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.auth.RegisterPartnerRequest;
import com.verisure.backend.dto.user.UserResponse;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.PartnerStatus;
import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.entity.enums.UserStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.PartnerRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación de {@link AuthService}.
 *
 * <p>No toca la base de datos: un JWT no tiene ninguna fila que borrar. El
 * cierre de sesión solo deja constancia en la traza, para quien la audite.
 * Quien descarta el token de verdad es el cliente, al recibir el 204.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PartnerRepository partnerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse registerPartner(RegisterPartnerRequest request) {
        String email = request.email();
        Optional<Partner> byCif = partnerRepository.findByCif(request.cif());

        // 1. ¿La persona ya tiene cuenta en esa misma entidad?
        if (byCif.isPresent()
                && userRepository.existsByEmailAndPartnerId(email, byCif.get().getId())) {
            throw new DomainException(ErrorCode.CIF_ALREADY_REGISTERED,
                    "El correo " + email + " ya tiene cuenta en " + byCif.get().getName());
        }

        // 2. ¿El correo ya está registrado en cualquier cuenta? No se repite.
        if (userRepository.existsByEmail(email)) {
            throw new DomainException(ErrorCode.EMAIL_ALREADY_REGISTERED,
                    "El correo " + email + " ya está registrado");
        }

        // 3. Deduplicación por CIF: si la entidad existe, no se crea otra.
        Partner partner = byCif.orElseGet(() -> {
            log.info("Nueva entidad con CIF {} en registro", request.cif());
            Partner nuevo = new Partner(
                    null,
                    request.name(),
                    request.cif(),
                    request.contactName(),
                    email,
                    request.phone(),
                    PartnerStatus.PENDING,
                    Instant.now(),
                    null,
                    null,
                    null);
            return partnerRepository.save(nuevo);
        });

        User user = new User();
        user.setFullName(request.contactName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.PARTNER);
        user.setPartner(partner);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCreatedAt(Instant.now());
        setNewVerificationToken(user);
        userRepository.save(user);

        log.info("Nuevo usuario {} pendiente de verificar en {}", email, partner.getName());
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void verify(String token) {
        if (token == null || token.isBlank()) {
            throw new DomainException(ErrorCode.VERIFICATION_EXPIRED,
                    "El enlace de verificación no es válido");
        }
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new DomainException(ErrorCode.VERIFICATION_EXPIRED,
                        "El enlace de verificación no es válido"));
        if (user.getVerifiedAt() != null) {
            throw new DomainException(ErrorCode.VERIFICATION_EXPIRED,
                    "Este enlace de verificación ya se ha utilizado");
        }
        if (user.getVerificationTokenCreatedAt() == null
                || user.getVerificationTokenCreatedAt().plus(User.VERIFICATION_TTL).isBefore(Instant.now())) {
            throw new DomainException(ErrorCode.VERIFICATION_EXPIRED,
                    "El enlace de verificación ha caducado");
        }
        user.setVerifiedAt(Instant.now());
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.PENDING_APPROVAL);
        }
        userRepository.save(user);
        log.info("Correo verificado de {}", user.getEmail());
    }

    @Override
    @Transactional
    public Long resendVerification(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            return null;
        }
        setNewVerificationToken(user);
        userRepository.save(user);
        log.info("Nuevo token de verificación para {}", email);
        return user.getId();
    }

    private void setNewVerificationToken(User user) {
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenCreatedAt(Instant.now());
    }

    @Override
    public void logout(String email) {
        log.info("Logout de {}", email);
    }
}