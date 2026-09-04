package com.verisure.backend.dto.user;

import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.Organization;
import com.verisure.backend.entity.enums.Role;

/**
 * Datos de la persona que devuelve el login y {@code GET /api/auth/me}.
 *
 * <p>{@code department} y {@code organization} son nulos para el rol
 * {@code PARTNER}: quien trabaja en una entidad colaboradora no pertenece ni a
 * Verisure España ni a Verisure Grupo.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String department,
        Organization organization) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment(),
                user.getOrganization());
    }
}
