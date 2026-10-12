package com.collins.platform.ledger.api.grpc;

import com.collins.platform.commons.api.Meta;
import com.collins.platform.commons.exception.PlatformException;
import com.collins.platform.ledger.application.BalanceService;
import com.collins.platform.ledger.application.BalanceView;
import com.collins.platform.ledger.application.PostingService;
import com.collins.platform.ledger.application.TransactionView;
import com.collins.platform.ledger.domain.Direction;
import com.collins.platform.ledger.domain.EntryCommand;
import com.collins.platform.ledger.domain.PostingCommand;
import com.collins.platform.ledger.grpc.BalanceResponse;
import com.collins.platform.ledger.grpc.EntryLine;
import com.collins.platform.ledger.grpc.GetBalanceRequest;
import com.collins.platform.ledger.grpc.LedgerServiceGrpc;
import com.collins.platform.ledger.grpc.PostTransactionPayload;
import com.collins.platform.ledger.grpc.PostTransactionRequest;
import com.collins.platform.ledger.grpc.TransactionIdRequest;
import com.collins.platform.ledger.grpc.TransactionResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

/** gRPC adapter for ledger.v1: translates protobuf messages to the same application services REST uses. */
@Component
public class LedgerGrpcService extends LedgerServiceGrpc.LedgerServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(LedgerGrpcService.class);

    private final PostingService posting;
    private final BalanceService balances;

    public LedgerGrpcService(PostingService posting, BalanceService balances) {
        this.posting = posting;
        this.balances = balances;
    }

    @Override
    public void postTransaction(PostTransactionRequest request, StreamObserver<TransactionResponse> observer) {
        handle(observer, () -> {
            PostTransactionPayload p = request.getPayload();
            PostingCommand command = new PostingCommand(
                    p.getIdempotencyKey(), p.getEntityId(), p.getTransactionType(), p.getDescription(),
                    p.getEffectiveDate().isBlank() ? null : LocalDate.parse(p.getEffectiveDate()),
                    blankToNull(p.getReferenceType()), blankToNull(p.getReferenceId()), p.getPending(),
                    p.getEntriesList().stream().map(e -> new EntryCommand(e.getAccount(), Direction.valueOf(e.getDirection()),
                            e.getAmountMinor(), e.getCurrency(), e.getEnforceNonNegative())).toList(),
                    p.getMetadataMap());
            return transactionResponse(posting.post(command));
        });
    }

    @Override
    public void commitPending(TransactionIdRequest request, StreamObserver<TransactionResponse> observer) {
        handle(observer, () -> transactionResponse(posting.commitPending(request.getPayload().getTransactionId())));
    }

    @Override
    public void voidPending(TransactionIdRequest request, StreamObserver<TransactionResponse> observer) {
        handle(observer, () -> transactionResponse(posting.voidPending(request.getPayload().getTransactionId())));
    }

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<BalanceResponse> observer) {
        handle(observer, () -> {
            BalanceView b = balances.current(request.getPayload().getEntityId(), request.getPayload().getAccount(),
                    request.getPayload().getCurrency());
            return BalanceResponse.newBuilder().setMeta(meta()).setPayload(com.collins.platform.ledger.grpc.BalanceView.newBuilder()
                    .setEntityId(b.entityId()).setAccount(b.account()).setCurrency(b.currency())
                    .setPostedMinor(b.postedMinor()).setPendingMinor(b.pendingMinor()).setAvailableMinor(b.availableMinor())
                    .build()).build();
        });
    }

    private static TransactionResponse transactionResponse(TransactionView v) {
        com.collins.platform.ledger.grpc.TransactionView.Builder view = com.collins.platform.ledger.grpc.TransactionView.newBuilder()
                .setTransactionId(v.transactionId()).setEntityId(v.entityId()).setStatus(v.status())
                .setTransactionType(v.transactionType()).setEffectiveDate(v.effectiveDate().toString());
        v.entries().forEach(e -> view.addEntries(EntryLine.newBuilder().setAccount(e.account()).setDirection(e.direction())
                .setAmountMinor(e.amountMinor()).setCurrency(e.currency()).build()));
        return TransactionResponse.newBuilder().setMeta(meta()).setPayload(view.build()).build();
    }

    private static com.collins.platform.ledger.grpc.Meta meta() {
        return com.collins.platform.ledger.grpc.Meta.newBuilder().setRequestId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toString()).setStatus(Meta.SUCCESS).build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static <T> void handle(StreamObserver<T> observer, Supplier<T> action) {
        try {
            observer.onNext(action.get());
            observer.onCompleted();
        } catch (PlatformException ex) {
            Status status = switch (ex.status()) {
                case 404 -> Status.NOT_FOUND;
                case 409 -> Status.ALREADY_EXISTS;
                case 400, 422 -> Status.FAILED_PRECONDITION;
                default -> Status.INTERNAL;
            };
            observer.onError(status.withDescription(ex.code() + ": " + ex.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException ex) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription("INVALID_ARGUMENT: " + ex.getMessage()).asRuntimeException());
        } catch (Exception ex) {
            log.error("gRPC call failed", ex);
            observer.onError(Status.INTERNAL.withDescription("INTERNAL_ERROR: " + ex.getMessage()).asRuntimeException());
        }
    }
}
