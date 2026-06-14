package com.afitnerd.tnra.billing.web.dto;

/**
 * Checkout request from a per-group app. {@code payerEmail} null/blank means self-pay; a different
 * value means {@code payerEmail} is gifting the subscription to {@code beneficiaryEmail}. The group
 * app is responsible for setting these from trusted OIDC identity (see the self-pay identity guard).
 */
public record CheckoutRequest(String beneficiaryEmail, String variant, String payerEmail) {}
