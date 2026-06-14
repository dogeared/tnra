package com.afitnerd.tnra.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BillingConfig {

    /** Injected wherever "now" is needed so trial / comp_until boundaries are testable. */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
