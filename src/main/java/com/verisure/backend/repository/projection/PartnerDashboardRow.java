package com.verisure.backend.repository.projection;

/**
 * Entidad con participación cerrada, para {@code partners.csv} · {@code B1-08}.
 *
 * <p>Es una fila por entidad y no por participación: cuántas actividades
 * distintas aportó y cuántas horas suman en total. Los contadores van en
 * {@code Long} porque es lo que Hibernate devuelve de {@code count}/{@code sum}
 * en una expresión de constructor.
 */
public record PartnerDashboardRow(
        Long partnerId,
        String partnerName,
        Long activityCount,
        Long totalHours) {
}