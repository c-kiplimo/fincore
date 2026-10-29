package com.collins.platform.ledger.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** A request to post (or hold as pending) one balanced journal transaction in one entity's books. */
public record PostingCommand(
        String idempotencyKey,
        String entityId,
        String transactionType,
        String description,
        LocalDate effectiveDate,
        String referenceType,
        String referenceId,
        boolean pending,
        List<EntryCommand> entries,
        Map<String, String> metadata) {
}
