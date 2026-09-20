package com.verisure.backend.dto.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/auth/register}: alta conjunta de entidad y persona. */
public record RegisterPartnerRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(min = 9, max = 9) String cif,
        @NotBlank @Size(max = 120) String contactName,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(min = 8) String password,
        @AssertTrue boolean consent) {
}