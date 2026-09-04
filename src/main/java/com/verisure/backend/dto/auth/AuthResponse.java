package com.verisure.backend.dto.auth;

import com.verisure.backend.dto.user.UserResponse;

/**
 * Respuesta de un login correcto.
 *
 * <p>{@code expiresIn} va en <b>segundos</b>, no en milisegundos: es lo que
 * espera frontend y lo que dice el contrato.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user) {

    public static AuthResponse of(String accessToken, long expiresInSeconds, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, user);
    }
}
