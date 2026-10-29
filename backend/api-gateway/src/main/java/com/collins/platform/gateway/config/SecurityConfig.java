package com.collins.platform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * With GATEWAY_SECURITY_ENABLED=true every /v1 call needs a valid JWT from the issuer (e.g. Keycloak).
 * Off by default so the project runs locally without an identity provider.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain security(ServerHttpSecurity http, @Value("${gateway.security.enabled:false}") boolean enabled) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);
        if (enabled) {
            http.authorizeExchange(ex -> ex.pathMatchers("/actuator/health/**").permitAll().anyExchange().authenticated())
                .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()));
        } else {
            http.authorizeExchange(ex -> ex.anyExchange().permitAll());
        }
        return http.build();
    }
}
