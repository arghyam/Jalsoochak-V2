package org.arghyam.jalsoochak.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    @Test
    void openApiListsGatewayRelativeLocalGatewayAndDirectLocal8087() {
        OpenAPI api = new OpenApiConfig().analyticsServiceOpenAPI();
        assertEquals(3, api.getServers().size());
        assertEquals("/analytics", api.getServers().get(0).getUrl());
        assertEquals("http://localhost:8080/analytics", api.getServers().get(1).getUrl());
        assertEquals("http://localhost:8087", api.getServers().get(2).getUrl());
    }
}
