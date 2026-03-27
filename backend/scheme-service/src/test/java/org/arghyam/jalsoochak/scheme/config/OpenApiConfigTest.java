package org.arghyam.jalsoochak.scheme.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenApiConfigTest {

    @Test
    void openApiListsGatewayLocalGatewayYmlPortAndReadmePort() {
        OpenAPI api = new OpenApiConfig().schemeServiceOpenAPI();
        assertEquals(4, api.getServers().size());
        assertEquals("/", api.getServers().get(0).getUrl());
        assertEquals("http://localhost:8080", api.getServers().get(1).getUrl());
        assertEquals("http://localhost:8287", api.getServers().get(2).getUrl());
        assertEquals("http://localhost:8086", api.getServers().get(3).getUrl());
    }
}
