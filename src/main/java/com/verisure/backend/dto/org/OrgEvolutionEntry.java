package com.verisure.backend.dto.org;

/**
 * Serie histórica de horas de una entidad · {@code B2-15}.
 *
 * <p>Un punto por año con participación cerrada. No depende del filtro de año:
 * el panel la pinta entera para que se vea la evolución.
 */
public record OrgEvolutionEntry(int year, long receivedHours) {
}
