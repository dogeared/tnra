package com.afitnerd.tnra.billing.web.dto;

/** One account a payer is covering (their gift list). */
public record CoveringEntry(String email, String status) {}
