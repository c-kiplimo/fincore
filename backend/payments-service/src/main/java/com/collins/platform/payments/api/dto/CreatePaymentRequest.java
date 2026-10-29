package com.collins.platform.payments.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(
    @NotBlank String merchantId,
    @Positive long amountMinor,
    @NotBlank String currency,
    @NotBlank @Pattern(regexp = "CARD|MOBILE_MONEY") String method,
    String instrument,
    String description
) {}