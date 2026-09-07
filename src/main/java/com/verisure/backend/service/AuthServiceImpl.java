package com.verisure.backend.service;

import org.springframework.stereotype.Service;

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

    @Override
    public void logout(String email) {
        log.info("Logout de {}", email);
    }
}
