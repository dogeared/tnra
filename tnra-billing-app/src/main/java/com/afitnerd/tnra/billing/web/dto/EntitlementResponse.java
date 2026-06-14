package com.afitnerd.tnra.billing.web.dto;

public record EntitlementResponse(boolean entitled, String status, String reason) {}
