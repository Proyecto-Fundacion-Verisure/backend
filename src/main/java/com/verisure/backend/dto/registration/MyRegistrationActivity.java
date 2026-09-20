package com.verisure.backend.dto.registration;

import java.time.LocalDate;

/**
 * La actividad tal y como se ve dentro de «Mis voluntariados».
 *
 * <p>No reutiliza {@code ActivitySummary}: aquel lleva estado, cupo y recuento
 * de «me gusta», y no lleva las horas, que es justo lo que la pantalla enseña.
 *
 * <p>Dueña: BE3 · Tarea: B3-06.
 */
public record MyRegistrationActivity(
        Long id,
        String title,
        String partner,
        LocalDate startDate,
        LocalDate endDate,
        Integer hours) {
}
