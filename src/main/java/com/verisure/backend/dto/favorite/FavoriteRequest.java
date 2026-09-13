package com.verisure.backend.dto.favorite;

import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /api/favorites}.
 *
 * <p>Dueña: BE3 · Tarea: B3-07.
 */
public record FavoriteRequest(@NotNull Long activityId) {
}
