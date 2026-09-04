package com.verisure.backend.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Responde <b>403</b> cuando la persona está identificada pero su rol no basta
 * para la ruta que ha pedido.
 *
 * <p>403 es «sé quién eres y no puedes». La distinción con el 401 es un criterio
 * de aceptación: si una empleada recibiera 401 al pedir el dashboard, la cadena
 * estaría mal montada.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter apiErrorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        apiErrorWriter.write(request, response, HttpStatus.FORBIDDEN,
                "FORBIDDEN", "No tienes permiso para esta operación");
    }
}
