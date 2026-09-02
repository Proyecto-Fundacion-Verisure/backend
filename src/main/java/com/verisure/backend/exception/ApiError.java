package com.verisure.backend.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Forma única de error de toda la API, incluidos los de validación por campo.
 *
 * <p>{@code fields} es {@code null} salvo en {@code VALIDATION_ERROR}. Es un
 * mapa de listas y no un mapa de cadenas a propósito: un mismo campo puede
 * incumplir dos validaciones a la vez —un CIF que falla {@code @Size} y
 * {@code @Pattern}— y con un solo mensaje por campo uno de los dos se pierde,
 * además de forma no determinista.
 *
 * <p>No lleva el código HTTP: ya viaja en la respuesta.
 */
public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path,
        Map<String, List<String>> fields) {

    public static ApiError of(String code, String message, String path) {
        return new ApiError(code, message, Instant.now(), path, null);
    }

    public static ApiError of(String code, String message, String path,
                              Map<String, List<String>> fields) {
        return new ApiError(code, message, Instant.now(), path, fields);
    }
}
