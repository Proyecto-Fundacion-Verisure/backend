package com.verisure.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Documentación viva de la API · {@code /swagger-ui.html}.
 *
 * <p>No sustituye al contrato: {@code docs/api-contract.md} sigue siendo la
 * fuente de verdad y explica las reglas de negocio que el esquema no puede
 * expresar. Aquí solo se declara cómo se llama la API y quién responde de ella.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI voluntariadoOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Voluntariado · Fundación Verisure")
                        .version("1.0.0")
                        .description("""
                                API del voluntariado corporativo: catálogo, inscripciones,
                                propuestas, cierres con certificado y dashboards de impacto.

                                El contrato completo, con las reglas de negocio de cada estado,
                                está en `docs/api-contract.md`.
                                """)
                        .license(new License().name("MIT")))
                .addServersItem(new Server().url("/").description("Servidor actual"));
    }
}
