package org.arghyam.jalsoochak.telemetry.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    @Test
    void openApiListsGatewayLocalGatewayYmlPortAndReadmePort() {
        OpenAPI api = new OpenApiConfig().telemetryServiceOpenAPI();
        assertEquals(4, api.getServers().size());
        assertEquals("/", api.getServers().get(0).getUrl());
        assertEquals("http://localhost:8080", api.getServers().get(1).getUrl());
        assertEquals("http://localhost:8089", api.getServers().get(2).getUrl());
        assertEquals("http://localhost:8084", api.getServers().get(3).getUrl());
    }
}
