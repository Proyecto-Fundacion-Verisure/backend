package com.verisure.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.service.AuthService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoints de sesión que no son el login (ese lo atiende el filtro
 * {@code JWTAuthentication} de la cadena de seguridad).
 *
 * <p>El logout responde 204 y solo deja traza. <b>No revoca el JWT y no
 * puede:</b> es sin estado, así que no hay ninguna fila que borrar. La
 * caducidad de dos horas <i>es</i> la política de revocación; el cierre de
 * sesión real lo hace el cliente al descartar el token.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Cierre de sesión. Requiere token (lo garantiza la cadena: cae en
     * {@code anyRequest().authenticated()}) y responde 204 sin cuerpo.
     *
     * <p>La identidad sale del {@code SecurityContext}; con ella se deja
     * constancia en la traza de quién salió.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        String email = currentEmail();
        authService.logout(email);
        return ResponseEntity.noContent().build();
    }

    /** El correo de quien llama, que es el nombre del contexto de seguridad. */
    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anónimo";
    }
}
