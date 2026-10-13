package com.afitnerd.tnra.billing.web.dto;

/** A payer asking to stop covering a member they gift a membership to. */
public record StopCoveringRequest(String payerEmail, String beneficiaryEmail) {}
