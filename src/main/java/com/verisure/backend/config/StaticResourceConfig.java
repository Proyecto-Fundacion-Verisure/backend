package com.verisure.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sirve los archivos subidos: las evidencias de los cierres y las portadas de las
 * actividades.
 *
 * <p>La carpeta {@code uploads/} está en {@code .gitignore}: cada persona tiene
 * los suyos y no se versionan.
 *
 * <p><b>Estas rutas piden token</b> —la regla está en
 * {@code SpringConfig}—, así que las portadas tampoco se ven sin haber entrado.
 * Es coherente con que el catálogo haya dejado de ser público.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}
