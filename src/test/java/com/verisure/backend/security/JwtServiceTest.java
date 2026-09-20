package com.verisure.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.verisure.backend.entity.enums.Role;

/** Firma, caducidad y manipulación del token · C-04. */
class JwtServiceTest {

    private static final String SECRET = "clave-de-test-suficientemente-larga-para-hmac256-0123456789";
    private static final String EMAIL = "ana.gil@verisure.ex";

    private final JwtService jwtService = new JwtService(SECRET, 7_200_000L);

    @Test
    void generateToken_carriesSubjectAndRole() {
        String token = jwtService.generateToken(EMAIL, Role.EMPLOYEE);

        Optional<DecodedJWT> decoded = jwtService.validate(token);

        assertTrue(decoded.isPresent());
        assertEquals(EMAIL, decoded.get().getSubject());
        assertEquals("EMPLOYEE", decoded.get().getClaim(JwtService.CLAIM_ROLE).asString());
    }

    @Test
    void validate_expiredToken_isEmpty() {
        JwtService alreadyExpired = new JwtService(SECRET, -60_000L);
        String token = alreadyExpired.generateToken(EMAIL, Role.EMPLOYEE);

        assertTrue(jwtService.validate(token).isEmpty());
    }

    @Test
    void validate_tokenSignedWithAnotherKey_isEmpty() {
        JwtService other = new JwtService("otra-clave-igual-de-larga-pero-distinta-9876543210abcdef", 7_200_000L);
        String token = other.generateToken(EMAIL, Role.ADMIN);

        assertTrue(jwtService.validate(token).isEmpty());
    }

    @Test
    void validate_tamperedToken_isEmpty() {
        String token = jwtService.generateToken(EMAIL, Role.EMPLOYEE);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertTrue(jwtService.validate(tampered).isEmpty());
    }

    @Test
    void validate_garbage_isEmpty() {
        assertTrue(jwtService.validate("no-es-un-jwt").isEmpty());
    }

    @Test
    void expiresInSeconds_convertsMillis() {
        assertEquals(7_200L, jwtService.expiresInSeconds());
    }
}
