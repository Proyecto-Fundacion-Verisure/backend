package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.user.UserResponse;
import com.verisure.backend.entity.User;
import com.verisure.backend.exception.NotFoundException;
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

    @Override
    @Transactional(readOnly = true)
    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
        return UserResponse.from(user);
    }

    @Override
    public void logout(String email) {
        log.info("Logout de {}", email);
    }
}
