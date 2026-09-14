package com.verisure.backend.repository.projection;

/**
 * Destinatario de un correo que no va ligado a una inscripción.
 *
 * <p>Dueña: BE3 · Tarea: B3-09.
 */
public record UserMailView(String userName, String userEmail) {
}
