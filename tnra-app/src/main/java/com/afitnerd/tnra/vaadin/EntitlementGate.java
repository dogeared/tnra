package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import com.afitnerd.tnra.service.OidcUserService;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

/**
 * Global navigation gate: a member whose billing isn't entitled is forwarded to {@link BillingView}.
 * Registered as a UI {@code BeforeEnterListener} by {@link MyServiceInitListener}.
 *
 * <pre>
 *   billing disabled (no client) ─► allow everything (self-host / billing off)
 *   not authenticated            ─► allow (login is handled elsewhere)
 *   target in ALLOWED set        ─► allow (profile + billing + welcome + error pages)
 *   entitled                     ─► allow
 *   else                         ─► forward to BillingView
 *   any error                    ─► allow (fail open — never trap a member out of a transient bug)
 * </pre>
 *
 * Entitlement itself fails open at the client when the billing service is unreachable, so a central
 * outage degrades to "everyone gets in," never "everyone locked out."
 */
@SpringComponent
public class EntitlementGate {

    private static final Logger log = LoggerFactory.getLogger(EntitlementGate.class);

    /** Routes a non-entitled member may still reach. */
    private static final Set<Class<?>> ALLOWED = Set.of(
        BillingView.class, BillingActivatingView.class, ProfileView.class, MainView.class,
        NotFoundView.class, ErrorView.class);

    private final transient Optional<BillingClient> billingClient;
    private final OidcUserService oidcUserService;

    public EntitlementGate(Optional<BillingClient> billingClient, OidcUserService oidcUserService) {
        this.billingClient = billingClient;
        this.oidcUserService = oidcUserService;
    }

    public void beforeEnter(BeforeEnterEvent event) {
        if (billingClient.isEmpty()) {
            return; // billing disabled
        }
        try {
            if (!oidcUserService.isAuthenticated()) {
                return;
            }
            if (ALLOWED.contains(event.getNavigationTarget())) {
                return;
            }
            String email = oidcUserService.getEmail();
            if (email == null) {
                return;
            }
            if (!billingClient.get().isEntitled(email)) {
                event.forwardTo(BillingView.class);
            }
        } catch (Exception e) {
            // Fail open: a bug in the gate must never lock members out of the app.
            log.warn("Entitlement gate error; allowing navigation: {}", e.getMessage());
        }
    }
}
