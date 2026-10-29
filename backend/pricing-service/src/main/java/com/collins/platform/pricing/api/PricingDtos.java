package com.collins.platform.pricing.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PricingDtos {

    private PricingDtos() {
    }

    public record CreateQuoteRequest(@NotBlank String entityId, @NotBlank String product, @Positive long amountMinor,
                                     @NotBlank String currency, String targetCurrency, String customerCountry,
                                     String customerType, LocalDate transactionDate) {
    }

    public record ConsumeQuoteRequest(@NotBlank String reference) {
    }

    public record TaxRuleRequest(@NotBlank String ruleCode, @NotBlank String jurisdiction, @NotBlank String product,
                                 @NotBlank String taxType, @NotNull BigDecimal rate, @NotBlank String ledgerAccount,
                                 boolean reverseChargeB2b, @NotNull LocalDate validFrom, boolean example,
                                 String description, String sourceUrl) {
    }

    public record FxRateRequest(@NotBlank String base, @NotBlank String quote, @NotNull BigDecimal midRate, int spreadBp) {
    }

    public record FxQuoteRequest(@Positive long amountMinor, @NotBlank String from, @NotBlank String to) {
    }

    public record ScheduleRequest(@Positive long principalMinor, @NotBlank String currency, @Positive int annualRateBp,
                                  @Positive int months, @NotNull LocalDate firstDueDate, String jurisdiction, String product) {
    }

    public record AccrueRequest(@Positive long principalMinor, @Positive int annualRateBp, @NotNull LocalDate from,
                                @NotNull LocalDate to, String dayCount, String jurisdiction, String product) {
    }

    public record AccrueResult(long principalMinor, int annualRateBp, LocalDate from, LocalDate to, String dayCount,
                               long interestMinor) {
    }
}
