package com.verisure.backend.repository.projection;

import java.time.Instant;

/**
 * Destinatario de un correo que no va ligado a una inscripción.
 *
 * <p>Lleva también el token de verificación y cuándo se generó, porque el
 * correo «verifica tu correo» los necesita para construir su enlace ·
 * Tarea: B3-09 y B1-15.
 */
public record UserMailView(String userName, String userEmail,
                           String verificationToken, Instant verificationCreatedAt) {
}
