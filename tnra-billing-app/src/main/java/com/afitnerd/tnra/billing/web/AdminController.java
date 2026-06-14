package com.afitnerd.tnra.billing.web;

import com.afitnerd.tnra.billing.config.BillingAppProperties;
import com.afitnerd.tnra.billing.model.GroupBilling;
import com.afitnerd.tnra.billing.service.GroupRegistrationService;
import com.afitnerd.tnra.billing.service.GroupRegistrationService.RegistrationResult;
import com.afitnerd.tnra.billing.util.HashUtil;
import com.afitnerd.tnra.billing.web.dto.GroupBillingUpdateRequest;
import com.afitnerd.tnra.billing.web.dto.GroupBillingView;
import com.afitnerd.tnra.billing.web.dto.RegisterGroupRequest;
import com.afitnerd.tnra.billing.web.dto.RegisterGroupResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Provisioning / admin API. Authenticated by a single shared {@code X-Admin-Token} header (the
 * provisioning operator), distinct from the per-group runtime tokens. Fails CLOSED: if no admin
 * token is configured, every admin call is rejected.
 *
 * <pre>
 *   POST  /api/admin/groups            register a group, mint + return its per-group token (once)
 *   PATCH /api/admin/groups/{slug}     update exempt / trial (pilot, promo)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final GroupRegistrationService registrationService;
    private final BillingAppProperties props;

    public AdminController(GroupRegistrationService registrationService, BillingAppProperties props) {
        this.registrationService = registrationService;
        this.props = props;
    }

    @PostMapping("/groups")
    public RegisterGroupResponse register(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @RequestBody RegisterGroupRequest request) {
        requireAdmin(adminToken);
        String slug = normalizeSlug(request.groupSlug());
        RegistrationResult result = registrationService.register(slug, request.trialDays(), request.exempt());
        GroupBilling group = result.group();
        return new RegisterGroupResponse(group.getGroupSlug(), result.token(), group.getCompUntil());
    }

    @PatchMapping("/groups/{slug}")
    public GroupBillingView update(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @PathVariable String slug,
            @RequestBody GroupBillingUpdateRequest request) {
        requireAdmin(adminToken);
        GroupBilling group = registrationService.update(
            normalizeSlug(slug), request.exempt(), request.trialDays());
        return new GroupBillingView(group.getGroupSlug(), Boolean.TRUE.equals(group.getExempt()),
            group.getCompUntil());
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "groupSlug is required");
        }
        return slug.trim().toLowerCase();
    }

    private void requireAdmin(String adminToken) {
        if (props.getAdminToken() == null || props.getAdminToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin API not configured");
        }
        if (!HashUtil.constantTimeEquals(props.getAdminToken(), adminToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin token");
        }
    }
}
