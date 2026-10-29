package com.collins.platform.ledger.api.rest;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.ledger.api.rest.LedgerDtos.PostTransactionRequest;
import com.collins.platform.ledger.api.rest.LedgerDtos.ReverseRequest;
import com.collins.platform.ledger.application.PostingService;
import com.collins.platform.ledger.application.TransactionView;
import com.collins.platform.ledger.domain.Direction;
import com.collins.platform.ledger.domain.EntryCommand;
import com.collins.platform.ledger.domain.PostingCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Manual journal entries and transaction lookups. Services normally post over gRPC instead. */
@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

    private final PostingService posting;

    public TransactionController(PostingService posting) {
        this.posting = posting;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionView> post(@RequestHeader(value = "Idempotency-Key", required = false) String headerKey,
                                             @Valid @RequestBody ApiRequest<PostTransactionRequest> request) {
        PostTransactionRequest p = request.payload();
        String key = headerKey != null ? headerKey : p.idempotencyKey();
        List<EntryCommand> entries = p.entries().stream().map(e -> new EntryCommand(e.account(), direction(e.direction()),
                e.amountMinor(), e.currency(), e.enforceNonNegative())).toList();
        PostingCommand command = new PostingCommand(key, p.entityId(), p.transactionType(), p.description(), p.effectiveDate(),
                p.referenceType(), p.referenceId(), p.pending(), entries, p.metadata() == null ? Map.of() : p.metadata());
        return ApiResponse.ok(posting.post(command));
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionView> get(@PathVariable String transactionId) {
        return ApiResponse.ok(posting.get(transactionId));
    }

    @GetMapping
    public ApiResponse<List<TransactionView>> list(@RequestParam(required = false) String entityId,
                                                   @RequestParam(required = false) String referenceId,
                                                   @RequestParam(defaultValue = "50") int limit,
                                                   @RequestParam(defaultValue = "0") int offset) {
        if (referenceId != null) {
            return ApiResponse.ok(posting.byReference(entityId, referenceId));
        }
        if (entityId == null) {
            throw new BusinessRuleException("FILTER_REQUIRED", "Give entityId or referenceId");
        }
        return ApiResponse.ok(posting.recent(entityId, Math.min(limit, 500), offset));
    }

    @PostMapping("/{transactionId}/commit")
    public ApiResponse<TransactionView> commit(@PathVariable String transactionId) {
        return ApiResponse.ok(posting.commitPending(transactionId));
    }

    @PostMapping("/{transactionId}/void")
    public ApiResponse<TransactionView> voidPending(@PathVariable String transactionId) {
        return ApiResponse.ok(posting.voidPending(transactionId));
    }

    @PostMapping("/{transactionId}/reverse")
    public ApiResponse<TransactionView> reverse(@PathVariable String transactionId,
                                                @RequestBody(required = false) ApiRequest<ReverseRequest> request) {
        ReverseRequest p = request == null || request.payload() == null ? new ReverseRequest(null, null) : request.payload();
        return ApiResponse.ok(posting.reverse(transactionId, p.reason(), p.effectiveDate()));
    }

    private static Direction direction(String value) {
        try {
            return Direction.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_DIRECTION", "direction must be DEBIT or CREDIT");
        }
    }
}
