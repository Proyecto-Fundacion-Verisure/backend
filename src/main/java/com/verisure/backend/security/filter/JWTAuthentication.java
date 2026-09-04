package com.verisure.backend.security.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.verisure.backend.dto.auth.AuthResponse;
import com.verisure.backend.dto.auth.LoginRequest;
import com.verisure.backend.dto.user.UserResponse;
import com.verisure.backend.security.AccountStatusException;
import com.verisure.backend.security.ApiErrorWriter;
import com.verisure.backend.security.JwtService;
import com.verisure.backend.security.UserDetail;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@code POST /api/auth/login}. Es el único sitio donde se comprueban las
 * credenciales y se emite un token.
 *
 * <p><b>Ojo con la clase que extiende.</b>
 * {@code UsernamePasswordAuthenticationFilter} viene preparada para formularios
 * HTML y lee {@code username} y {@code password} como parámetros de formulario.
 * El contrato manda <b>JSON</b> con {@code email} y {@code password}, así que
 * {@link #attemptAuthentication} está sobrescrito para deserializar el cuerpo. Sin
 * eso, el login recibiría credenciales nulas y fallaría siempre sin decir por qué.
 */
public class JWTAuthentication extends UsernamePasswordAuthenticationFilter {

    public static final String LOGIN_URL = "/api/auth/login";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final ApiErrorWriter apiErrorWriter;

    public JWTAuthentication(AuthenticationManager authenticationManager,
                             JwtService jwtService,
                             ObjectMapper objectMapper,
                             ApiErrorWriter apiErrorWriter) {
        super(authenticationManager);
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.apiErrorWriter = apiErrorWriter;
        setFilterProcessesUrl(LOGIN_URL);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequest credentials =
                    objectMapper.readValue(request.getInputStream(), LoginRequest.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            credentials.email(), credentials.password()));

        } catch (IOException e) {
            throw new UnreadableBodyException();
        }
    }

    /** Credenciales correctas y cuenta activa: se emite el token. */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {

        UserDetail userDetail = (UserDetail) authResult.getPrincipal();
        var user = userDetail.getUser();

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        AuthResponse body = AuthResponse.of(
                token, jwtService.expiresInSeconds(), UserResponse.from(user));

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Aquí aterrizan los tres códigos de estado de cuenta.
     *
     * <p>Un fallo de credenciales devuelve <b>401 genérico</b>, sin distinguir si
     * el correo existe: decir «ese correo no está registrado» permitiría averiguar
     * quién tiene cuenta probando direcciones.
     */
    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException {

        if (failed instanceof AccountStatusException accountStatus) {
            apiErrorWriter.write(request, response, accountStatus.getErrorCode().status(),
                    accountStatus.getErrorCode().name(), accountStatus.getMessage());
            return;
        }

        if (failed instanceof UnreadableBodyException) {
            apiErrorWriter.write(request, response, HttpStatus.BAD_REQUEST,
                    "MALFORMED_REQUEST", "El cuerpo de la petición no se puede leer");
            return;
        }

        apiErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED", "Credenciales no válidas");
    }

    /** El cuerpo del login no era JSON válido. */
    private static class UnreadableBodyException extends AuthenticationException {
        UnreadableBodyException() {
            super("El cuerpo de la petición no se puede leer");
        }
    }
}
