package org.arghyam.jalsoochak.scheme.config;

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
    public OpenAPI schemeServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scheme Service API")
                        .description("Microservice responsible for scheme management in the JalSoochak platform.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("JalSoochak Team")))
                .servers(List.of(
                        new Server().url("/scheme").description("API gateway (same origin as docs URL)"),
                        new Server().url("http://localhost:8080/scheme").description("Local API gateway (:8080)"),
                        new Server().url("http://localhost:8287").description("Local service (direct, this application.yml)"),
                        new Server().url("http://localhost:8086").description("Local service (README default :8086)")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components().addSecuritySchemes("Bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
