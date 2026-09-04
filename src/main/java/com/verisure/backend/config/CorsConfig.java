package com.verisure.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Permite que el navegador deje a Vite llamar a esta API.
 *
 * <p>Dos líneas que parecen de relleno y no lo son:
 * <ul>
 *   <li><b>{@code OPTIONS}</b>: el navegador manda una comprobación previa antes
 *       de cualquier {@code POST} con cabecera {@code Authorization}. Si no está
 *       permitida, frontend ve un error de CORS que no dice nada útil.</li>
 *   <li><b>{@code Content-Disposition} expuesta</b>: sin ella, las descargas de
 *       CSV y PDF del dashboard llegan sin nombre de archivo.</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
