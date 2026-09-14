package com.verisure.backend.repository.projection;

import java.time.LocalDate;

/**
 * Todo lo que una plantilla de correo necesita saber de una inscripción.
 *
 * <p>Una sola proyección para los siete avisos de participación: viene con el
 * destinatario, la actividad y los dos datos que solo usan algunos correos
 * —{@code queuePosition} para la lista de espera y {@code closureId} para el
 * enlace al certificado—, porque una consulta por aviso sería la misma consulta
 * cinco veces.
 *
 * <p>Dueña: BE3 · Tarea: B3-09.
 */
public record RegistrationMailView(
        Long registrationId,
        String userName,
        String userEmail,
        Long activityId,
        String activityTitle,
        LocalDate startDate,
        LocalDate endDate,
        Integer queuePosition,
        Long closureId) {
}
