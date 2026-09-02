package com.verisure.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Habilita {@code @Async} en toda la aplicación.
 *
 * <p>Sin esta clase, <b>{@code @Async} se ignora en silencio</b>: los métodos
 * anotados se ejecutan en el hilo de la petición, el correo bloquea la
 * respuesta y no hay ningún aviso de que está pasando.
 *
 * <p>La usa {@link com.verisure.backend.service.MailDispatcher}.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
