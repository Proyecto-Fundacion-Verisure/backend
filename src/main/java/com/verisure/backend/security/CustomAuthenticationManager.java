package com.verisure.backend.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.verisure.backend.entity.User;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Comprueba las credenciales del login contra la base de datos.
 *
 * <p>El orden de las dos comprobaciones —primero la contraseña, después el estado
 * de la cuenta— <b>es deliberado y es una medida de seguridad</b>. Si el estado se
 * mirara antes, cualquiera podría probar correos al azar y averiguar cuáles están
 * registrados y en qué situación, sin conocer ninguna contraseña. Comprobando
 * primero la contraseña, esa información solo la ve quien ya ha demostrado ser esa
 * persona.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationManager implements AuthenticationManager {

    /** Mismo texto para correo inexistente y contraseña incorrecta, a propósito. */
    private static final String INVALID_CREDENTIALS = "Credenciales no válidas";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = String.valueOf(authentication.getPrincipal());
        String password = String.valueOf(authentication.getCredentials());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        // Solo ahora, con la contraseña ya acertada, se dice en qué estado está.
        checkAccountStatus(user);

        UserDetail userDetail = new UserDetail(user);
        return new UsernamePasswordAuthenticationToken(
                userDetail, null, userDetail.getAuthorities());
    }

    private void checkAccountStatus(User user) {
        switch (user.getStatus()) {
            case PENDING_VERIFICATION -> throw new AccountStatusException(
                    ErrorCode.ACCOUNT_NOT_VERIFIED,
                    "Falta confirmar el correo electrónico");
            case PENDING_APPROVAL -> throw new AccountStatusException(
                    ErrorCode.ACCOUNT_PENDING_APPROVAL,
                    "La Fundación aún no ha aprobado la cuenta");
            case REJECTED -> throw new AccountStatusException(
                    ErrorCode.ACCOUNT_REJECTED,
                    "La cuenta fue rechazada");
            case ACTIVE -> { /* puede entrar */ }
        }
    }
}
