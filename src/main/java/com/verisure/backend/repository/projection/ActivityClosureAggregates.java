package com.verisure.backend.repository.projection;

/**
 * Totales de participación de una actividad, para la pantalla de cierre.
 *
 * <p>Existe para que {@code ActivityClosureService} <b>no tenga que llamar</b> a
 * {@code ParticipationClosureService}: los agregados se leen directamente del
 * repositorio del otro cierre, que es una dependencia de datos y no de dominio.
 *
 * <p>Las otras dos cifras de la pantalla no salen de aquí: los voluntarios
 * confirmados los da
 * {@code registrationRepository.countByActivityIdAndStatus(id, CONFIRMED)} —el
 * único sitio donde BE1 usa esa firma cruzada— y las horas previstas son
 * {@code activity.hours × confirmados}, una multiplicación en el servicio.
 *
 * <p>{@code reportedHours} es {@code Long} y no {@code Integer} porque en JPQL
 * {@code sum()} sobre un entero devuelve {@code Long}, y una expresión de
 * constructor exige que los tipos coincidan exactamente.
 */
public record ActivityClosureAggregates(
        Long reportedHours,
        long closedParticipations,
        long evidenceCount) {
}
