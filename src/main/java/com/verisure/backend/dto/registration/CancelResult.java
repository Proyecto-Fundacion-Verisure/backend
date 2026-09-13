package com.verisure.backend.dto.registration;

/**
 * Lo que devuelve el servicio al cancelar: la inscripción y a quién ascendió.
 *
 * <p>El destinatario de {@code notifySpotReleased} nace dentro de la transacción
 * y su identificador no está en la ruta ni en el cuerpo, así que el controlador
 * no puede adivinarlo y el servicio se lo tiene que decir. A {@code null}, no
 * ascendió nadie y no hay aviso que mandar.
 *
 * <p>Dueña: BE3 · Tarea: B3-06.
 */
public record CancelResult(RegistrationResponse body, Long promotedRegistrationId) {
}
