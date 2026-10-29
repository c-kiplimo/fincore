package com.collins.platform.pricing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** The priced breakdown of one transaction. Amounts in minor units of {@code currency} unless stated. */
public record QuoteModel(
        String quoteId,
        String entityId,
        String jurisdiction,
        String product,
        String currency,
        long amountMinor,
        String feePayer,
        List<Fee> fees,
        List<Tax> taxes,
        Fx fx,
        long totalFeesMinor,
        long totalTaxesMinor,
        long totalDebitedMinor,
        long netToRecipientMinor,
        String status,
        Instant expiresAt) {

    public record Fee(String type, long amountMinor) {
    }

    public record Tax(String taxType, String jurisdiction, String ruleCode, int ruleVersion, BigDecimal rate,
                      long baseMinor, long amountMinor, String ledgerAccount, boolean reverseCharge, boolean example) {
    }

    /** spreadRevenueMinor and convertedMinor are in the target currency. */
    public record Fx(String fromCurrency, String toCurrency, BigDecimal midRate, BigDecimal customerRate, int spreadBp,
                     long midConvertedMinor, long convertedMinor, long spreadRevenueMinor) {
    }

    public QuoteModel withStatus(String newStatus) {
        return new QuoteModel(quoteId, entityId, jurisdiction, product, currency, amountMinor, feePayer, fees, taxes, fx,
                totalFeesMinor, totalTaxesMinor, totalDebitedMinor, netToRecipientMinor, newStatus, expiresAt);
    }
}
