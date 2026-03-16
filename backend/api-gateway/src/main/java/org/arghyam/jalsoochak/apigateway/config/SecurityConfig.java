package org.arghyam.jalsoochak.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        // Public auth endpoints (forwarded to user-service)
                        .pathMatchers(
                                "/user/api/v1/auth/login",
                                "/user/api/v1/auth/refresh",
                                "/user/api/v1/auth/logout",
                                "/user/api/v1/auth/invite/info",
                                "/user/api/v1/auth/activate-account",
                                "/user/api/v1/auth/forgot-password",
                                "/user/api/v1/auth/reset-password",
                                "/user/api/v1/public/**"
                        ).permitAll()
                        // Gateway and service health / metrics
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()
                        // All other routes require a valid JWT
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(org.springframework.security.config.Customizer.withDefaults())
                );

        return http.build();
    }
}
