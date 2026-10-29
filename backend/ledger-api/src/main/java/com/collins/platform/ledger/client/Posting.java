package com.collins.platform.ledger.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for a ledger posting, used by every service that moves money.
 * <pre>
 * Posting.of("KE", "PAYMENT_CAPTURE", "payment:pi_1:capture")
 *     .reference("payment_intent", "pi_1")
 *     .debit("1100", 100000, "KES")
 *     .credit("2000:mer_1", 97100, "KES")
 *     .credit("4000", 2900, "KES");
 * </pre>
 */
public final class Posting {

    public record Line(String account, String direction, long amountMinor, String currency, boolean enforceNonNegative) {
    }

    private final String entityId;
    private final String transactionType;
    private final String idempotencyKey;
    private String description = "";
    private String effectiveDate = "";
    private String referenceType = "";
    private String referenceId = "";
    private boolean pending;
    private final List<Line> lines = new ArrayList<>();
    private final Map<String, String> metadata = new LinkedHashMap<>();

    private Posting(String entityId, String transactionType, String idempotencyKey) {
        this.entityId = entityId;
        this.transactionType = transactionType;
        this.idempotencyKey = idempotencyKey;
    }

    public static Posting of(String entityId, String transactionType, String idempotencyKey) {
        return new Posting(entityId, transactionType, idempotencyKey);
    }

    public Posting description(String description) {
        this.description = description;
        return this;
    }

    public Posting effectiveDate(String isoDate) {
        this.effectiveDate = isoDate;
        return this;
    }

    public Posting reference(String type, String id) {
        this.referenceType = type;
        this.referenceId = id;
        return this;
    }

    public Posting pending() {
        this.pending = true;
        return this;
    }

    public Posting meta(String key, String value) {
        this.metadata.put(key, value);
        return this;
    }

    public Posting debit(String account, long amountMinor, String currency) {
        return line(account, "DEBIT", amountMinor, currency, false);
    }

    public Posting credit(String account, long amountMinor, String currency) {
        return line(account, "CREDIT", amountMinor, currency, false);
    }

    /** Debit that must not push the account's natural balance below zero (e.g. a wallet). */
    public Posting debitNoOverdraft(String account, long amountMinor, String currency) {
        return line(account, "DEBIT", amountMinor, currency, true);
    }

    private Posting line(String account, String direction, long amountMinor, String currency, boolean enforce) {
        if (amountMinor > 0) {
            lines.add(new Line(account, direction, amountMinor, currency, enforce));
        }
        return this;
    }

    public String entityId() { return entityId; }
    public String transactionType() { return transactionType; }
    public String idempotencyKey() { return idempotencyKey; }
    public String description() { return description; }
    public String effectiveDate() { return effectiveDate; }
    public String referenceType() { return referenceType; }
    public String referenceId() { return referenceId; }
    public boolean isPending() { return pending; }
    public List<Line> lines() { return lines; }
    public Map<String, String> metadata() { return metadata; }
}
