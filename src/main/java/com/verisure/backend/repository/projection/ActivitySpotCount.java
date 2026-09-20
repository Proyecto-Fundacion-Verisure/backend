package com.verisure.backend.repository.projection;

/**
 * Plazas cubiertas de una actividad: quien tiene plaza en ella.
 *
 * <p>Se pide por lotes, una vez por página de catálogo, porque preguntarlo
 * actividad a actividad convierte una rejilla de veinte tarjetas en veinte
 * consultas.
 *
 * <p>Dueña: BE3 · Tarea: B2-07.
 */
public record ActivitySpotCount(Long activityId, Long occupied) {
}
