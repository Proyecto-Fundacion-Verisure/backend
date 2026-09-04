package com.verisure.backend.security;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.verisure.backend.entity.enums.Role;

/**
 * Firma y valida los tokens. <b>No consulta la base de datos</b>: solo hace
 * criptografía. De saber quién es la persona se encarga
 * {@link CustomAuthenticationManager}.
 *
 * <p>Usa <b>{@code java-jwt} de Auth0</b>, que es la librería que trae el
 * {@code pom.xml}. El runbook da los ejemplos con {@code jjwt}: ese código no
 * compila aquí.
 *
 * <p>La caducidad es la política de revocación completa del proyecto, porque
 * {@code POST /api/auth/logout} no revoca nada: responde 204 y deja traza.
 */
@Service
public class JwtService {

    /** Nombre del claim donde viaja el rol, para no repetir la cadena suelta. */
    public static final String CLAIM_ROLE = "role";

    private final Algorithm algorithm;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.algorithm = Algorithm.HMAC256(secret);
        this.expirationMs = expirationMs;
    }

    /** Lo llama el login, una sola vez, cuando las credenciales son correctas. */
    public String generateToken(String email, Role role) {
        Instant ahora = Instant.now();
        return JWT.create()
                .withSubject(email)
                .withClaim(CLAIM_ROLE, role.name())
                .withIssuedAt(ahora)
                .withExpiresAt(ahora.plusMillis(expirationMs))
                .sign(algorithm);
    }

    /**
     * Lo llama el filtro en cada petición.
     *
     * <p>Devuelve vacío en vez de lanzar porque un token caducado es el caso
     * normal a las dos horas, no un error del programa: así el filtro lo trata
     * igual que la ausencia de token y no hay excepciones volando dentro de la
     * cadena de Spring Security, donde el manejador global no llega.
     */
    public Optional<DecodedJWT> validate(String token) {
        try {
            return Optional.of(JWT.require(algorithm).build().verify(token));
        } catch (JWTVerificationException e) {
            return Optional.empty();   // firma inválida, manipulado o caducado
        }
    }

    /** Segundos que dura el token, que es lo que espera {@code AuthResponse}. */
    public long expiresInSeconds() {
        return expirationMs / 1000;
    }
}
