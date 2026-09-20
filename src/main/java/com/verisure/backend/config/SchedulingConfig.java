package com.verisure.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita {@code @Scheduled} en toda la aplicación.
 *
 * <p>Sin esta clase, {@code @Scheduled} se ignora en silencio: la tarea
 * no se ejecuta nunca y nada avisa de que no lo hace.
 *
 * <p>La usa {@link com.verisure.backend.scheduler.ActivityStatusScheduler}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
