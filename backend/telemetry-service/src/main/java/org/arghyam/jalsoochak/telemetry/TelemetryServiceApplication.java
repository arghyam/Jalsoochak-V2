package org.arghyam.jalsoochak.telemetry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class TelemetryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelemetryServiceApplication.class, args);
    }
}
