package com.verisure.backend.dto.registration;

import com.verisure.backend.entity.enums.RegistrationStatus;

/**
 * Una línea de «Mis voluntariados».
 *
 * <p>{@code closureId} y {@code activityClosed} son los que deciden qué botón
 * pinta la pantalla: sin cierre, «cerrar tu participación»; con cierre y la
 * actividad abierta, solo lectura; con las dos cosas, «descargar certificado».
 * {@code activityClosed} es redundante con {@code status}, y va igualmente para
 * que frontend no tenga que conocer la máquina de estados.
 *
 * <p>{@code accepted} solo dice algo cuando el estado es {@code WAITLISTED}: separa
 * a quien sigue esperando a que administración la revise de quien ya pasó por ahí y
 * está en cola porque no había hueco. Es lo único que distingue las dos esperas, y
 * es justo lo que quiere saber quien aguarda plaza.
 *
 * <p>Dueña: BE3 · Tarea: B3-06.
 */
public record MyRegistrationItem(
        Long registrationId,
        MyRegistrationActivity activity,
        RegistrationStatus status,
        boolean accepted,
        Integer queuePosition,
        Long closureId,
        boolean activityClosed) {
}
