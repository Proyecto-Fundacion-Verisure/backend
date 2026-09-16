package com.verisure.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Cuerpo de {@code POST /api/auth/resend-verification} · {@code B1-15}. */
public record ResendVerificationRequest(@NotBlank @Email String email) {
}