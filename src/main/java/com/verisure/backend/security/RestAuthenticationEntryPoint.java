package com.verisure.backend.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Responde <b>401</b> cuando la petición no viene identificada: sin token, o con
 * uno inválido, manipulado o caducado.
 *
 * <p>401 es «no sé quién eres». Si además hiciera falta un rol concreto, eso lo
 * responde {@link RestAccessDeniedHandler} con un 403.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorWriter apiErrorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        apiErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Se necesita autenticación para esta operación");
    }
}
