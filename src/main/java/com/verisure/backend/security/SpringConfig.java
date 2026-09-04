package com.verisure.backend.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verisure.backend.security.filter.JWTAuthentication;
import com.verisure.backend.security.filter.JWTAuthorization;

import lombok.RequiredArgsConstructor;

/**
 * La cadena de seguridad, las rutas públicas y el reparto por rol.
 *
 * <p>Sin esta clase, Spring Boot aplica su cadena por defecto y protege
 * <b>todos</b> los endpoints con autenticación básica y una contraseña generada
 * en cada arranque: la aplicación levanta, pero frontend no puede llamar a nada.
 *
 * <p>La sesión es {@code STATELESS} y CSRF está desactivado. No es una dejadez:
 * la protección CSRF existe para sesiones con cookies, y aquí la identidad viaja
 * en una cabecera que el navegador no adjunta solo, así que no hay nada que
 * falsificar desde otro sitio.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // habilita @PreAuthorize en los controladores
@RequiredArgsConstructor
public class SpringConfig {

    private final CustomAuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final ApiErrorWriter apiErrorWriter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JWTAuthentication login =
                new JWTAuthentication(authenticationManager, jwtService, objectMapper, apiErrorWriter);

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)   // 401 con ApiError
                        .accessDeniedHandler(accessDeniedHandler))            // 403 con ApiError
                .authorizeHttpRequests(auth -> auth

                        // ── Públicas ─────────────────────────────────────────
                        .requestMatchers(POST, "/api/auth/login",
                                               "/api/auth/register",
                                               "/api/auth/resend-verification").permitAll()
                        .requestMatchers(GET, "/api/auth/verify").permitAll()
                        // El formulario de la landing: una entidad puede proponer
                        // sin tener cuenta. Es la única pública de negocio.
                        .requestMatchers(POST, "/api/proposals").permitAll()

                        // ── Catálogo · ya no es público ──────────────────────
                        // Una entidad no ve el catálogo general: solo lo suyo,
                        // en /api/org/activities.
                        .requestMatchers(GET, "/api/activities", "/api/activities/*")
                                .hasAnyRole("EMPLOYEE", "ADMIN")

                        // ── Por rol ──────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/org/**").hasRole("PARTNER")
                        .requestMatchers("/uploads/**").authenticated()

                        // Las rutas que sirven a dos roles llegan aquí: quién
                        // puede lo decide el servicio con NOT_OWNER.
                        .anyRequest().authenticated())

                .addFilter(login)
                .addFilterBefore(new JWTAuthorization(jwtService),
                                 UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
