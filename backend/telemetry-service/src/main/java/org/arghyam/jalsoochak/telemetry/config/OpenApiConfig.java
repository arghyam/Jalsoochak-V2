package org.arghyam.jalsoochak.telemetry.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI telemetryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Telemetry Service API")
                        .description("Microservice responsible for telemetry ingestion and meter reading workflows "
                                + "in the JalSoochak platform.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("JalSoochak Team")))
                .servers(List.of(
                        new Server().url("/").description("API gateway (same origin as docs URL)"),
                        new Server().url("http://localhost:8080").description("Local API gateway (:8080)"),
                        new Server().url("http://localhost:8089").description("Local service (direct, this application.yml)"),
                        new Server().url("http://localhost:8084").description("Local service (README default :8084)")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components().addSecuritySchemes("Bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
