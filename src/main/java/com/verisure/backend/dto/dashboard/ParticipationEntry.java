package com.verisure.backend.dto.dashboard;

/**
 * Personas participantes por organización o por línea de acción · {@code B1-07}.
 *
 * <p>{@code participants} cuenta personas distintas, no participaciones, igual
 * que {@link DepartmentEntry}. Lleva {@code id} y {@code label} porque el valor
 * en crudo ({@code VERISURE_ES}, {@code desoledad}) no es legible en un gráfico.
 */
public record ParticipationEntry(String id, String label, long participants) {
}
