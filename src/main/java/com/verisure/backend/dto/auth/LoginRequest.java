package com.verisure.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de {@code POST /api/auth/login}. Viaja como JSON, no como formulario. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {
}
