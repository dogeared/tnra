package com.afitnerd.tnra.billing.web.dto;

/**
 * Checkout request from a per-group app. {@code payerEmail} null/blank means self-pay; a different
 * value means {@code payerEmail} is gifting the subscription to {@code beneficiaryEmail}. The group
 * app is responsible for setting these from trusted OIDC identity (see the self-pay identity guard).
 *
 * {@code redirectUrl} is where Lemon Squeezy sends the buyer after a successful checkout — the group
 * app's own URL, so the member lands back in their app rather than on the Lemon Squeezy store.
 * Null/blank falls back to the store's default redirect.
 */
public record CheckoutRequest(String beneficiaryEmail, String variant, String payerEmail,
                              String redirectUrl) {}
