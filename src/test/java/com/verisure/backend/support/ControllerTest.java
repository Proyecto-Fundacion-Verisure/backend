package com.verisure.backend.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.TestPropertySource;

import com.verisure.backend.config.CorsConfig;
import com.verisure.backend.security.ApiErrorWriter;
import com.verisure.backend.security.CustomAuthenticationManager;
import com.verisure.backend.security.JwtService;
import com.verisure.backend.security.PasswordEncoderConfig;
import com.verisure.backend.security.RestAccessDeniedHandler;
import com.verisure.backend.security.RestAuthenticationEntryPoint;
import com.verisure.backend.security.SpringConfig;

/**
 * Test de capa web con la cadena de seguridad real y los servicios mockeados.
 *
 * <p>{@code @WebMvcTest} solo carga controladores y {@code @ControllerAdvice};
 * la seguridad se importa aquí para que cada test pase por los filtros JWT y
 * por {@code SpringConfig} de verdad. {@code CustomAuthenticationManager}
 * necesita un {@code UserRepository}: cada test lo declara con
 * {@code @MockitoBean}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebMvcTest
@Import({
        SpringConfig.class,
        JwtService.class,
        ApiErrorWriter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        CustomAuthenticationManager.class,
        PasswordEncoderConfig.class,
        CorsConfig.class,
        AuthHelper.class })
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-test-suficientemente-larga-para-hmac256-0123456789",
        "app.jwt.expiration-ms=7200000",
        "app.cors.allowed-origin=http://localhost:5173" })
public @interface ControllerTest {

    /** Controladores que se cargan; vacío carga todos. */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
