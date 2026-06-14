package com.afitnerd.tnra.billing.web.dto;

/** Admin update of a group's billing posture (pilot/promo). Both fields optional. */
public record GroupBillingUpdateRequest(Boolean exempt, Integer trialDays) {}
