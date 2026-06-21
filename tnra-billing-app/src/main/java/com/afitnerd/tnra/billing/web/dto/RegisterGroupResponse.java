package com.afitnerd.tnra.billing.web.dto;

import java.time.LocalDateTime;

/** {@code apiToken} is the plaintext per-group token — shown ONCE; only its hash is stored. */
public record RegisterGroupResponse(String groupSlug, String apiToken, LocalDateTime trialEndsAt) {}
