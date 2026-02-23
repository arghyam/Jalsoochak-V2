package com.example.tenant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tenantServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tenant Service API")
                        .description("Microservice responsible for tenant onboarding, schema provisioning, "
                                + "and tenant configuration management in the JalSoochak platform.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("JalSoochak Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local development")));
    }
}
