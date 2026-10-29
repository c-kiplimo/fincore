package com.collins.platform.ledger.api.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request payloads for the ledger REST API (each sits inside {"payload": ...}). */
public final class LedgerDtos {

    private LedgerDtos() {
    }

    public record EntryRequest(
            @NotBlank String account,
            @NotBlank String direction,
            @Positive long amountMinor,
            @NotBlank @Size(min = 3, max = 3) String currency,
            boolean enforceNonNegative) {
    }

    public record PostTransactionRequest(
            String idempotencyKey,
            @NotBlank String entityId,
            @NotBlank String transactionType,
            String description,
            LocalDate effectiveDate,
            String referenceType,
            String referenceId,
            boolean pending,
            @NotEmpty @Valid List<EntryRequest> entries,
            Map<String, String> metadata) {
    }

    public record ReverseRequest(String reason, LocalDate effectiveDate) {
    }

    public record CreateEntityRequest(
            @NotBlank @Size(max = 10) String entityId,
            @NotBlank String name,
            @NotBlank @Size(min = 2, max = 2) String country,
            @NotBlank @Size(min = 3, max = 3) String functionalCurrency,
            @NotBlank String timezone,
            @NotBlank String copyChartFrom) {
    }

    public record RevaluationRequest(@NotNull LocalDate asOf, @NotEmpty Map<String, BigDecimal> rates) {
    }

    public record ConsolidationRequest(
            @NotEmpty List<String> entityIds,
            @NotBlank String reportingCurrency,
            @NotNull LocalDate asOf,
            @NotEmpty Map<String, BigDecimal> closingRates,
            Map<String, BigDecimal> averageRates) {
    }
}
