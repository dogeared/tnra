package com.afitnerd.tnra.landing.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // VaadinSecurityConfigurer reads @AnonymousAllowed on each view to grant
        // anonymous access. LandingView is annotated, so nothing extra is needed.
        // Explicitly calling `authorizeHttpRequests(...).anyRequest()` before
        // VaadinSecurityConfigurer.init() runs trips a "Can't configure
        // requestMatchers after anyRequest" assertion in Vaadin 24.9.11+.
        http.with(VaadinSecurityConfigurer.vaadin(), cfg -> {});
        return http.build();
    }
}
