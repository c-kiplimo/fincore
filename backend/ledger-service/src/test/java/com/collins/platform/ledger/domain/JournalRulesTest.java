package com.collins.platform.ledger.domain;

import com.collins.platform.commons.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalRulesTest {

    private static PostingCommand command(List<EntryCommand> entries) {
        return new PostingCommand("key-1", "KE", "TEST", "", LocalDate.now(), null, null, false, entries, Map.of());
    }

    @Test
    void acceptsBalancedCapture() {
        // Dr Settlement receivable 1,000 / Cr Merchant payable 971 / Cr Fee revenue 29
        assertThatCode(() -> JournalRules.validate(command(List.of(
                new EntryCommand("1100", Direction.DEBIT, 100_000, "KES", false),
                new EntryCommand("2000:mer_1", Direction.CREDIT, 97_100, "KES", false),
                new EntryCommand("4000", Direction.CREDIT, 2_900, "KES", false))))).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnbalanced() {
        assertThatThrownBy(() -> JournalRules.validate(command(List.of(
                new EntryCommand("1100", Direction.DEBIT, 100_000, "KES", false),
                new EntryCommand("2000:mer_1", Direction.CREDIT, 99_999, "KES", false)))))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("differ");
    }

    @Test
    void balancesEachCurrencySeparately() {
        // USD 100 in, KES 13,000 out, joined by the FX clearing account in both currencies
        assertThatCode(() -> JournalRules.validate(command(List.of(
                new EntryCommand("1100", Direction.DEBIT, 10_000, "USD", false),
                new EntryCommand("1400", Direction.CREDIT, 10_000, "USD", false),
                new EntryCommand("1400", Direction.DEBIT, 1_300_000, "KES", false),
                new EntryCommand("2000:mer_1", Direction.CREDIT, 1_300_000, "KES", false))))).doesNotThrowAnyException();
    }

    @Test
    void randomBalancedTransactionsAlwaysPass() {
        Random random = new Random(42);
        for (int i = 0; i < 1_000; i++) {
            long a = 1 + random.nextInt(1_000_000);
            long b = 1 + random.nextInt(1_000_000);
            assertThatCode(() -> JournalRules.validate(command(List.of(
                    new EntryCommand("1000", Direction.DEBIT, a + b, "KES", false),
                    new EntryCommand("2010:w1", Direction.CREDIT, a, "KES", false),
                    new EntryCommand("4300", Direction.CREDIT, b, "KES", false))))).doesNotThrowAnyException();
        }
    }
}
