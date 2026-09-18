package com.verisure.backend.support;

import org.springframework.stereotype.Component;

import com.verisure.backend.entity.enums.Role;
import com.verisure.backend.security.JwtService;

import lombok.RequiredArgsConstructor;

/**
 * Cabecera {@code Authorization} con un token real, para que el test pase por
 * el filtro JWT en vez de saltárselo con {@code @WithMockUser}.
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final JwtService jwtService;

    public String bearer(Role role) {
        return bearer(role.name().toLowerCase() + "@verisure.ex", role);
    }

    public String bearer(String email, Role role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }
}
