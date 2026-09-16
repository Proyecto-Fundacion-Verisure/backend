package com.verisure.backend.dto.dashboard;

/**
 * Una fila del ranking de las diez actividades con más «me gusta».
 *
 * <p>{@code favoriteCount} es {@code Long} y no {@code long} porque la consulta
 * lo construye con la expresión de constructor de JPQL, y {@code count()}
 * devuelve {@code Long}: los tipos tienen que coincidir exactamente.
 */
public record FavoriteRankingEntry(Long activityId, String activityTitle, Long favoriteCount) {
}