package com.verisure.backend.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verisure.backend.exception.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Escribe un {@link ApiError} directamente en la respuesta.
 *
 * <p>Hace falta porque Spring Security resuelve sus errores <b>dentro de la
 * cadena de filtros</b>, antes del {@code DispatcherServlet}, así que
 * {@code GlobalExceptionHandler} —que es un {@code @RestControllerAdvice}— nunca
 * los ve. Sin esto, un 401 llegaría con el cuerpo por defecto de Spring y la
 * forma única de error que exige el contrato se rompería justo en el caso más
 * frecuente.
 *
 * <p>Lo usan los tres sitios que responden errores de seguridad: los dos
 * manejadores de la cadena y el fallo de login.
 */
@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String code, String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError body = ApiError.of(code, message, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
