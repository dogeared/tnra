package com.afitnerd.tnra.billing.web.dto;

import java.time.Instant;

/**
 * One account a payer is covering (their gift list).
 * @param cancelAt when a scheduled cancellation of that gift takes effect, or null if none
 */
public record CoveringEntry(String email, String status, Instant cancelAt) {}
