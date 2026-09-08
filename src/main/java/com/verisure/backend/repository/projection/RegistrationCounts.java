package com.verisure.backend.repository.projection;

/**
 * Contadores del tablero de una actividad: confirmadas, en cola y sin revisar.
 *
 * <p>«Sin revisar» son las que siguen en cola y nadie ha decidido todavía. Una
 * rechazada también tiene {@code accepted = false}, pero sí está revisada, así
 * que el contador filtra además por {@code WAITLISTED}.
 *
 * <p>Dueña: BE3 · Tarea: B3-03.
 */
public record RegistrationCounts(
        Long confirmed,
        Long waitlisted,
        Long unreviewed) {
}
