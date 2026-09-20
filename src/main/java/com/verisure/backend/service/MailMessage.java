package com.verisure.backend.service;

import java.util.Map;

/**
 * Un correo ya resuelto: a quién va, qué plantilla lo pinta y con qué datos.
 *
 * <p>{@code critical} solo cambia el nivel del registro cuando el envío falla:
 * el correo de verificación deja a una entidad bloqueada si no llega, y ese
 * fallo tiene que verse entre los avisos normales.
 *
 * <p>Dueña: BE3 · Tarea: B3-09.
 */
public record MailMessage(
        String recipient,
        String subject,
        String template,
        Map<String, Object> variables,
        boolean critical) {

    public static MailMessage of(String recipient, String subject, String template,
                                 Map<String, Object> variables) {
        return new MailMessage(recipient, subject, template, variables, false);
    }
}
