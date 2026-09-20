package com.verisure.backend.dto.registration;

/**
 * Cuerpo opcional de la cancelación.
 *
 * <p>El motivo no se persiste: queda en la traza del servicio. Ninguna
 * pantalla ni consulta del proyecto lo lee, así que una columna sería un campo
 * muerto. El campo existe porque está en el contrato de API.
 *
 * <p>Dueña: BE3 · Tarea: B3-06.
 */
public record CancelRequest(String reason) {
}
