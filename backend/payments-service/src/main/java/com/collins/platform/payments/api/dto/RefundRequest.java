package com.collins.platform.payments.api.dto;

public record RefundRequest(
    long amountMinor,
    String reason
) {}