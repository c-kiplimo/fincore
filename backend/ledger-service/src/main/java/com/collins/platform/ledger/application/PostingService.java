package com.collins.platform.ledger.application;

import com.collins.platform.commons.events.Topics;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.ConflictException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.outbox.OutboxPublisher;
import com.collins.platform.ledger.domain.Direction;
import com.collins.platform.ledger.domain.EntryCommand;
import com.collins.platform.ledger.domain.JournalRules;
import com.collins.platform.ledger.domain.PostingCommand;
import com.collins.platform.ledger.infrastructure.LedgerRepository;
import com.collins.platform.ledger.infrastructure.LedgerRows.AccountRef;
import com.collins.platform.ledger.infrastructure.LedgerRows.BalanceRow;
import com.collins.platform.ledger.infrastructure.LedgerRows.ChartAccount;
import com.collins.platform.ledger.infrastructure.LedgerRows.EntryRow;
import com.collins.platform.ledger.infrastructure.LedgerRows.TxnRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Posts journal transactions. This is where the platform's money rules are enforced:
 * balanced entries, idempotency, closed periods, overdraft protection and append-only history.
 */
@Service
public class PostingService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyy-MM");

    private final LedgerRepository repo;
    private final OutboxPublisher outbox;
    private final ObjectMapper mapper;

    public PostingService(LedgerRepository repo, OutboxPublisher outbox, ObjectMapper mapper) {
        this.repo = repo;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional
    public TransactionView post(PostingCommand command) {
        JournalRules.validate(command);
        repo.entity(command.entityId()).orElseThrow(() -> new NotFoundException("Legal entity", command.entityId()));

        String hash = hash(command);
        Optional<TxnRow> existing = repo.transactionByKey(command.entityId(), command.idempotencyKey());
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(hash)) {
                throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                        "Idempotency key " + command.idempotencyKey() + " was already used for a different transaction");
            }
            return view(existing.get());
        }

        LocalDate effectiveDate = command.effectiveDate() == null ? LocalDate.now() : command.effectiveDate();
        if (repo.periodClosed(command.entityId(), effectiveDate.format(PERIOD))) {
            throw new BusinessRuleException("PERIOD_CLOSED",
                    "Period " + effectiveDate.format(PERIOD) + " is closed for " + command.entityId() + "; post into an open period");
        }

        // Resolve (get-or-create) every account and remember its normal side.
        List<AccountRef> refs = new ArrayList<>();
        for (EntryCommand entry : command.entries()) {
            ChartAccount chart = repo.chartAccount(command.entityId(), entry.baseCode())
                    .orElseThrow(() -> new BusinessRuleException("UNKNOWN_ACCOUNT",
                            "Account code " + entry.baseCode() + " is not in the chart of accounts of " + command.entityId()));
            refs.add(repo.account(command.entityId(), entry.account(), entry.baseCode(), entry.currency(), chart.normalSide()));
        }

        // Lock every touched balance row in a fixed order, then check overdraft rules.
        Map<Long, BalanceRow> locked = repo.lockBalances(new TreeSet<>(refs.stream().map(AccountRef::id).toList()));
        Map<Long, Long> deltaByAccount = new HashMap<>();
        for (int i = 0; i < command.entries().size(); i++) {
            EntryCommand entry = command.entries().get(i);
            deltaByAccount.merge(refs.get(i).id(), entry.direction().signed(entry.amountMinor()), Math::addExact);
        }
        for (int i = 0; i < command.entries().size(); i++) {
            EntryCommand entry = command.entries().get(i);
            if (!entry.enforceNonNegative()) {
                continue;
            }
            AccountRef ref = refs.get(i);
            BalanceRow row = locked.get(ref.id());
            long newNet = row.postedNet() + row.pendingNet() + deltaByAccount.get(ref.id());
            if (JournalRules.natural(newNet, ref.normalSide()) < 0) {
                throw new BusinessRuleException("INSUFFICIENT_FUNDS",
                        "Account " + ref.account() + " (" + ref.currency() + ") does not have enough funds");
            }
        }

        String status = command.pending() ? "PENDING" : "POSTED";
        TxnRow txn = new TxnRow(Ids.newId("txn"), command.entityId(), command.idempotencyKey(), hash,
                command.transactionType(), command.description(), effectiveDate, command.referenceType(),
                command.referenceId(), status, null, null);
        repo.insertTransaction(txn, json(command.metadata()));
        for (int i = 0; i < command.entries().size(); i++) {
            EntryCommand entry = command.entries().get(i);
            repo.insertEntry(txn.id(), refs.get(i).id(), i + 1, entry.direction().name(), entry.amountMinor(), entry.currency());
        }
        deltaByAccount.forEach((accountId, delta) -> {
            if (command.pending()) {
                repo.addToBalance(accountId, 0, delta);
            } else {
                repo.addToBalance(accountId, delta, 0);
            }
        });

        TransactionView view = view(repo.transaction(txn.id(), false).orElseThrow());
        if (!command.pending()) {
            publishPosted(view);
        }
        return view;
    }

    @Transactional
    public TransactionView commitPending(String transactionId) {
        TxnRow txn = repo.transaction(transactionId, true).orElseThrow(() -> new NotFoundException("Transaction", transactionId));
        if ("POSTED".equals(txn.status())) {
            return view(txn);
        }
        requireStatus(txn, "PENDING");
        List<EntryRow> entries = repo.entries(transactionId);
        Map<Long, Long> delta = netByAccount(entries);
        repo.lockBalances(new TreeSet<>(delta.keySet()));
        delta.forEach((accountId, d) -> repo.addToBalance(accountId, d, -d));
        repo.markStatus(transactionId, "POSTED");
        TransactionView view = view(repo.transaction(transactionId, false).orElseThrow());
        publishPosted(view);
        return view;
    }

    @Transactional
    public TransactionView voidPending(String transactionId) {
        TxnRow txn = repo.transaction(transactionId, true).orElseThrow(() -> new NotFoundException("Transaction", transactionId));
        if ("VOIDED".equals(txn.status())) {
            return view(txn);
        }
        requireStatus(txn, "PENDING");
        Map<Long, Long> delta = netByAccount(repo.entries(transactionId));
        repo.lockBalances(new TreeSet<>(delta.keySet()));
        delta.forEach((accountId, d) -> repo.addToBalance(accountId, 0, -d));
        repo.markStatus(transactionId, "VOIDED");
        return view(repo.transaction(transactionId, false).orElseThrow());
    }

    /** Never edit, always reverse: posts a new transaction with every entry flipped. */
    @Transactional
    public TransactionView reverse(String transactionId, String reason, LocalDate effectiveDate) {
        TxnRow original = repo.transaction(transactionId, true).orElseThrow(() -> new NotFoundException("Transaction", transactionId));
        requireStatus(original, "POSTED");
        List<EntryCommand> flipped = repo.entries(transactionId).stream()
                .map(e -> new EntryCommand(e.account(), Direction.valueOf(e.direction()).opposite(), e.amountMinor(), e.currency(), false))
                .toList();
        PostingCommand command = new PostingCommand("reversal:" + transactionId, original.entityId(), "REVERSAL",
                "Reversal of " + transactionId + (reason == null ? "" : ": " + reason),
                effectiveDate, original.referenceType(), original.referenceId(), false, flipped, Map.of("reversal_of", transactionId));
        TransactionView reversal = post(command);
        repo.linkReversal(reversal.transactionId(), transactionId);
        return get(reversal.transactionId());
    }

    public TransactionView get(String transactionId) {
        return view(repo.transaction(transactionId, false).orElseThrow(() -> new NotFoundException("Transaction", transactionId)));
    }

    public List<TransactionView> byReference(String entityId, String referenceId) {
        return repo.transactionsByReference(entityId, referenceId).stream().map(this::view).toList();
    }

    public List<TransactionView> recent(String entityId, int limit, int offset) {
        return repo.recentTransactions(entityId, limit, offset).stream().map(this::view).toList();
    }

    private void publishPosted(TransactionView view) {
        outbox.publish(Topics.LEDGER_TRANSACTION_POSTED, view.transactionId(), "LedgerTransactionPosted", view);
    }

    private static Map<Long, Long> netByAccount(List<EntryRow> entries) {
        Map<Long, Long> delta = new HashMap<>();
        for (EntryRow e : entries) {
            delta.merge(e.accountId(), Direction.valueOf(e.direction()).signed(e.amountMinor()), Math::addExact);
        }
        return delta;
    }

    private static void requireStatus(TxnRow txn, String expected) {
        if (!expected.equals(txn.status())) {
            throw new BusinessRuleException("INVALID_TRANSACTION_STATE",
                    "Transaction " + txn.id() + " is " + txn.status() + "; expected " + expected);
        }
    }

    TransactionView view(TxnRow t) {
        List<TransactionView.Line> lines = repo.entries(t.id()).stream()
                .map(e -> new TransactionView.Line(e.lineNo(), e.account(), e.direction(), e.amountMinor(), e.currency()))
                .toList();
        return new TransactionView(t.id(), t.entityId(), t.status(), t.transactionType(), t.description(), t.effectiveDate(),
                t.referenceType(), t.referenceId(), t.reversalOf(), t.createdAt(), lines);
    }

    private String hash(PostingCommand command) {
        try {
            String canonical = mapper.writeValueAsString(List.of(command.entityId(), command.transactionType(),
                    String.valueOf(command.pending()), command.entries()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String json(Map<String, String> metadata) {
        try {
            return metadata == null || metadata.isEmpty() ? null : mapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
