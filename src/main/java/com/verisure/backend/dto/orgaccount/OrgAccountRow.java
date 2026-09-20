package com.verisure.backend.dto.orgaccount;

import java.time.Instant;

import com.verisure.backend.entity.enums.UserStatus;

/**
 * Fila de administración de una cuenta de entidad · {@code B1-16}.
 *
 * <p>Lo que consume el frontend en {@code /admin/organizations}: los datos de
 * la persona de contacto y de su entidad, cuándo pidió el alta y si verificó
 * el correo.
 */
public record OrgAccountRow(
        Long id,
        String organizationName,
        String cif,
        String contactName,
        String email,
        String phone,
        Instant requestedAt,
        UserStatus status,
        boolean emailVerified) {
}