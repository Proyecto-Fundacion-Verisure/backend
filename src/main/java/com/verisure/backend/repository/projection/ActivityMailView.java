package com.verisure.backend.repository.projection;

/**
 * Datos de una actividad para los correos del rol entidad.
 *
 * <p>{@code contactEmail} es el de quien la creó y, si la creó administración,
 * el de la entidad: el aviso de aprobada o devuelta tiene que llegar a alguien
 * aunque la cuenta creadora no exista.
 *
 * <p>Dueña: BE3 · Tarea: B3-09.
 */
public record ActivityMailView(
        Long activityId,
        String title,
        String reviewNote,
        String partnerName,
        String contactName,
        String contactEmail) {
}
