package com.verisure.backend.dto.dashboard;

/**
 * Una tasa de eficacia del dashboard · {@code B1-07}.
 *
 * <p>{@code value} es un porcentaje de 0 a 100 y {@code id} un identificador
 * estable que el frontend usa como clave de pintado, igual que en la semilla
 * del frontend:
 *
 * <pre>
 *   workforce-participation   Voluntarios distintos / plantilla activa
 *   place-occupancy           Participaciones cerradas / plazas ofertadas
 *   registration-conversion   Participaciones cerradas / inscripciones creadas
 * </pre>
 */
public record EffectivenessMetric(String id, String label, double value) {

    /** Construye una tasa evitando {@code div/0} y porcentajes disparatados. */
    public static EffectivenessMetric of(String id, String label, long part, long total) {
        double value = total <= 0 ? 0 : part * 100.0 / total;
        return new EffectivenessMetric(id, label, Math.min(value, 100));
    }
}