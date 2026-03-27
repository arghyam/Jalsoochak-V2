package org.arghyam.jalsoochak.tenant.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    @Test
    void openApiListsGatewayRelativeLocalGatewayAndDirectLocal() {
        OpenAPI api = new OpenApiConfig().tenantServiceOpenAPI();
        assertEquals(3, api.getServers().size());
        assertEquals("/", api.getServers().get(0).getUrl());
        assertEquals("http://localhost:8080", api.getServers().get(1).getUrl());
        assertEquals("http://localhost:8081", api.getServers().get(2).getUrl());
    }
}
