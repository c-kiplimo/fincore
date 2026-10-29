package com.collins.platform.ledger.domain;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.money.CurrencyUnits;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The rules every journal transaction must obey before it exists. Pure domain code: no Spring, no SQL.
 * <ol>
 *   <li>At least two entries (double entry).</li>
 *   <li>Every amount is a positive whole number of minor units.</li>
 *   <li>Total debits equal total credits <b>in each currency separately</b>.</li>
 * </ol>
 */
public final class JournalRules {

    private static final Pattern ACCOUNT = Pattern.compile("^\\d{4}(:[A-Za-z0-9_\\-.]{1,100})?$");

    private JournalRules() {
    }

    public static void validate(PostingCommand command) {
        if (command.entityId() == null || command.entityId().isBlank()) {
            throw new BusinessRuleException("ENTITY_REQUIRED", "entityId is required");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw new BusinessRuleException("IDEMPOTENCY_KEY_REQUIRED", "idempotencyKey is required");
        }
        if (command.entries() == null || command.entries().size() < 2) {
            throw new BusinessRuleException("TOO_FEW_ENTRIES", "A transaction needs at least two entries (double entry)");
        }
        Map<String, Long> netByCurrency = new HashMap<>();
        for (EntryCommand entry : command.entries()) {
            if (entry.account() == null || !ACCOUNT.matcher(entry.account()).matches()) {
                throw new BusinessRuleException("INVALID_ACCOUNT", "Invalid account '" + entry.account() + "'; expected e.g. 1100 or 2000:mer_1");
            }
            if (entry.amountMinor() <= 0) {
                throw new BusinessRuleException("INVALID_AMOUNT", "Entry amounts must be positive minor units");
            }
            if (entry.currency() == null || !CurrencyUnits.supported(entry.currency())) {
                throw new BusinessRuleException("UNSUPPORTED_CURRENCY", "Unsupported currency " + entry.currency());
            }
            netByCurrency.merge(entry.currency(), entry.direction().signed(entry.amountMinor()), Math::addExact);
        }
        netByCurrency.forEach((currency, net) -> {
            if (net != 0) {
                throw new BusinessRuleException("TRANSACTION_NOT_BALANCED",
                        "Debits and credits differ by " + net + " minor units of " + currency);
            }
        });
    }

    /** Natural balance: positive when the account sits on its normal side. */
    public static long natural(long debitPositiveNet, String normalSide) {
        return "DEBIT".equals(normalSide) ? debitPositiveNet : -debitPositiveNet;
    }
}
