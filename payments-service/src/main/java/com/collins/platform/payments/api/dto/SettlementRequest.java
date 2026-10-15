package com.collins.platform.payments.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SettlementRequest(
    @NotBlank String entityId,
    @NotBlank String currency,
    LocalDate date
) {}