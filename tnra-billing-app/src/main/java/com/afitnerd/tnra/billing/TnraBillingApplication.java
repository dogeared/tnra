package com.afitnerd.tnra.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Central billing service for TNRA.
 *
 * Multi-tenancy is physical (one MySQL DB + Keycloak realm + subdomain per group), and Lemon
 * Squeezy is a single store with a single webhook URL. A per-group app instance cannot answer
 * "which group's database does this payment event belong to?" — so this service is the one
 * central authority that owns the Lemon Squeezy relationship and the entitlement source of truth.
 * Per-group {@code tnra-app} instances call this service's API; they hold no billing state.
 */
@SpringBootApplication
public class TnraBillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TnraBillingApplication.class, args);
    }
}
