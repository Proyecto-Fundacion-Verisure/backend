package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.User;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * La entidad de quien llama, resuelta desde la sesión.
 *
 * <p>Vive en un solo sitio a propósito: es la regla de seguridad de todo
 * {@code /api/org/**} y el {@code partnerId} nunca llega por parámetro.
 *
 * <p>Dueña: BE3 · Tarea: B2-13.
 */
@Service
@RequiredArgsConstructor
public class PartnerResolver {

    private final UserRepository userRepository;

    /** La entidad a la que pertenece la cuenta autenticada. */
    @Transactional(readOnly = true)
    public Partner resolve(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + userEmail));

        Partner partner = user.getPartner();
        if (partner == null) {
            throw new NotFoundException(
                    "La cuenta de " + userEmail + " no pertenece a ninguna entidad");
        }
        return partner;
    }
}
