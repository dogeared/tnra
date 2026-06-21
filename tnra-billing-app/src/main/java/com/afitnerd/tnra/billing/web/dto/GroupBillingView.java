package com.afitnerd.tnra.billing.web.dto;

import java.time.LocalDateTime;

public record GroupBillingView(String groupSlug, boolean exempt, LocalDateTime compUntil) {}
