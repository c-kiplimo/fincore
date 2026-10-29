package com.collins.platform.commons.clients;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Client-side copy of pricing-service's quote (the published language between services). */
public record Quote(
        String quoteId, String entityId, String jurisdiction, String product, String currency, long amountMinor,
        String feePayer, List<Fee> fees, List<Tax> taxes, Fx fx, long totalFeesMinor, long totalTaxesMinor,
        long totalDebitedMinor, long netToRecipientMinor, String status, Instant expiresAt) {

    public record Fee(String type, long amountMinor) {
    }

    public record Tax(String taxType, String jurisdiction, String ruleCode, int ruleVersion, BigDecimal rate,
                      long baseMinor, long amountMinor, String ledgerAccount, boolean reverseCharge, boolean example) {

        /** Tax payable account in the ledger, kept per jurisdiction, e.g. "2510:KE". */
        public String account() {
            return ledgerAccount + ":" + jurisdiction;
        }
    }

    public record Fx(String fromCurrency, String toCurrency, BigDecimal midRate, BigDecimal customerRate, int spreadBp,
                     long midConvertedMinor, long convertedMinor, long spreadRevenueMinor) {
    }
}
