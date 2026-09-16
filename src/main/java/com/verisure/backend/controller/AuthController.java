package com.verisure.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.auth.RegisterPartnerRequest;
import com.verisure.backend.dto.auth.ResendVerificationRequest;
import com.verisure.backend.dto.user.UserResponse;
import com.verisure.backend.service.AuthService;
import com.verisure.backend.service.NotificationService;

import jakarta.validation.Valid;

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
    private final NotificationService notificationService;

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

     /** {@code GET /api/auth/me}. Perfil de quien hace la petición. */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return authService.me(authentication.getName());
    }

    /**
     * {@code POST /api/auth/register}. Alta conjunta de entidad y persona.
     *
     * <p>Ruta pública, no emite token: la cuenta nace sin verificar y el login ya
     * la bloquea hasta que confirme el correo. Se dispara el aviso de verificación
     * desde fuera de la transacción.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterPartnerRequest request) {
        UserResponse created = authService.registerPartner(request);
        notificationService.notifyVerificationRequested(created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code GET /api/auth/verify}. Valida el correo desde el enlace del aviso.
     *
     * <p>Ruta pública, sin sesión. Sigue viva si la token sirvió ya (falla con
     * {@code VERIFICATION_EXPIRED}, llámese como se llame el enlace). Responde
     * 200 y deja la cuenta pendiente de aprobación.
     */
    @GetMapping("/verify")
    public ResponseEntity<Void> verify(@RequestParam String token) {
        authService.verify(token);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code POST /api/auth/resend-verification}. Manda otro enlace.
     *
     * <p>Ruta pública. No enumera correos: si la cuenta no existe o ya no está
     * pendiente de verificar responde 204 sin emitir aviso.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        Long userId = authService.resendVerification(request.email());
        if (userId != null) {
            notificationService.notifyVerificationRequested(userId);
        }
        return ResponseEntity.noContent().build();
    }
}
