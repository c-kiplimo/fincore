package com.collins.platform.ledger.client;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.ConflictException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.exception.UpstreamException;
import com.collins.platform.ledger.grpc.EntryLine;
import com.collins.platform.ledger.grpc.GetBalancePayload;
import com.collins.platform.ledger.grpc.GetBalanceRequest;
import com.collins.platform.ledger.grpc.BalanceView;
import com.collins.platform.ledger.grpc.LedgerServiceGrpc;
import com.collins.platform.ledger.grpc.PostTransactionPayload;
import com.collins.platform.ledger.grpc.PostTransactionRequest;
import com.collins.platform.ledger.grpc.TransactionIdPayload;
import com.collins.platform.ledger.grpc.TransactionIdRequest;
import com.collins.platform.ledger.grpc.TransactionResponse;
import com.collins.platform.ledger.grpc.TransactionView;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The only way services move money: a thin wrapper over the ledger.v1 gRPC stub that maps
 * gRPC status codes back to the platform's exceptions.
 */
public class LedgerClient {

    private final LedgerServiceGrpc.LedgerServiceBlockingStub stub;

    public LedgerClient(ManagedChannel channel) {
        this.stub = LedgerServiceGrpc.newBlockingStub(channel);
    }

    public LedgerTransaction post(Posting posting) {
        PostTransactionPayload.Builder payload = PostTransactionPayload.newBuilder()
                .setIdempotencyKey(posting.idempotencyKey())
                .setEntityId(posting.entityId())
                .setTransactionType(posting.transactionType())
                .setDescription(posting.description())
                .setEffectiveDate(posting.effectiveDate())
                .setReferenceType(posting.referenceType())
                .setReferenceId(posting.referenceId())
                .setPending(posting.isPending())
                .putAllMetadata(posting.metadata());
        for (Posting.Line line : posting.lines()) {
            payload.addEntries(EntryLine.newBuilder()
                    .setAccount(line.account())
                    .setDirection(line.direction())
                    .setAmountMinor(line.amountMinor())
                    .setCurrency(line.currency())
                    .setEnforceNonNegative(line.enforceNonNegative())
                    .build());
        }
        TransactionResponse response = call(() -> deadline().postTransaction(
                PostTransactionRequest.newBuilder().setPayload(payload).build()));
        return toTransaction(response.getPayload());
    }

    public LedgerTransaction commitPending(String transactionId) {
        return toTransaction(call(() -> deadline().commitPending(idRequest(transactionId))).getPayload());
    }

    public LedgerTransaction voidPending(String transactionId) {
        return toTransaction(call(() -> deadline().voidPending(idRequest(transactionId))).getPayload());
    }

    public LedgerBalance balance(String entityId, String account, String currency) {
        BalanceView view = call(() -> deadline().getBalance(GetBalanceRequest.newBuilder()
                .setPayload(GetBalancePayload.newBuilder()
                        .setEntityId(entityId).setAccount(account).setCurrency(currency).build())
                .build())).getPayload();
        return new LedgerBalance(view.getEntityId(), view.getAccount(), view.getCurrency(),
                view.getPostedMinor(), view.getPendingMinor(), view.getAvailableMinor());
    }

    private LedgerServiceGrpc.LedgerServiceBlockingStub deadline() {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private static TransactionIdRequest idRequest(String transactionId) {
        return TransactionIdRequest.newBuilder()
                .setPayload(TransactionIdPayload.newBuilder().setTransactionId(transactionId).build())
                .build();
    }

    private static LedgerTransaction toTransaction(TransactionView view) {
        return new LedgerTransaction(view.getTransactionId(), view.getEntityId(), view.getStatus(), view.getTransactionType());
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (StatusRuntimeException ex) {
            Status status = ex.getStatus();
            String description = status.getDescription() == null ? "" : status.getDescription();
            String code = description.contains(":") ? description.substring(0, description.indexOf(':')) : "LEDGER_ERROR";
            String message = description.contains(":") ? description.substring(description.indexOf(':') + 1).trim() : description;
            switch (status.getCode()) {
                case INVALID_ARGUMENT, FAILED_PRECONDITION -> throw new BusinessRuleException(code, message);
                case NOT_FOUND -> throw new NotFoundException("Ledger record", message);
                case ALREADY_EXISTS, ABORTED -> throw new ConflictException(code, message);
                default -> throw new UpstreamException("LEDGER_UNAVAILABLE", "ledger-service: " + status.getCode() + " " + message, 503, ex);
            }
        }
    }
}
