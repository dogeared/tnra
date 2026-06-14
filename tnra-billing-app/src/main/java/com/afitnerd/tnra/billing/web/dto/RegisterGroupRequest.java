package com.afitnerd.tnra.billing.web.dto;

/** Provisioning request. trialDays/exempt optional (trialDays defaults to billing.trial-days). */
public record RegisterGroupRequest(String groupSlug, Integer trialDays, Boolean exempt) {}
