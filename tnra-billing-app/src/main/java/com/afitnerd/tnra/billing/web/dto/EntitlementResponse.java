package com.afitnerd.tnra.billing.web.dto;

/** {@code payerEmail} is who pays for this member (null for self-pay; set for a gifted membership). */
public record EntitlementResponse(boolean entitled, String status, String reason, String payerEmail) {}
