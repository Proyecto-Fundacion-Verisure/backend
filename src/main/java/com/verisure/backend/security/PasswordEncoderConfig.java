package com.verisure.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * El codificador de contraseñas, en su propia clase.
 *
 * <p><b>Está separado de {@link SpringConfig} a propósito.</b> Si el <i>bean</i>
 * viviera allí, se formaría un ciclo: {@code SpringConfig} necesita
 * {@link CustomAuthenticationManager} para montar el filtro de login, y el manager
 * necesita el codificador para comparar la contraseña. Spring no puede resolver
 * esa dependencia circular y el contexto no arranca.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
