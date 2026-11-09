package com.verisure.backend.dto.org;

/**
 * Horas repartidas por línea de acción en una entidad · {@code B2-15}.
 *
 * <p>{@code value} son horas, no personas: el panel de la entidad mira cuánto
 * voluntariado aporta cada línea. {@code id} y {@code label} separan el valor
 * en crudo ({@code desoledad}) de lo que se lee en el gráfico.
 */
public record OrgDistributionEntry(String id, String label, long value) {
}
