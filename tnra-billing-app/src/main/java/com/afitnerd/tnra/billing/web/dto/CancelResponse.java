package com.afitnerd.tnra.billing.web.dto;

import java.time.Instant;

/** @param endsAt when the (scheduled) cancellation takes effect — access continues until then */
public record CancelResponse(Instant endsAt) {}
