package com.collins.platform.simulator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SimulatorModels {

    private SimulatorModels() {
    }

    public record AuthorizeRequest(String paymentId, long amountMinor, String currency, String method, String instrument) {
    }

    public record AuthorizeResult(String networkRef, String status, String reason) {
    }

    public record NetworkRefRequest(String networkRef, long amountMinor, String reason) {
    }

    public record NetworkEvent(String networkRef, String paymentId, String type, long grossMinor, long feeMinor, String currency) {
    }

    public record SettlementRequest(String currency, LocalDate date) {
    }

    public record SettlementBatch(String batchId, String currency, LocalDate date, List<NetworkEvent> lines,
                                  long grossMinor, long feesMinor, long netMinor) {
    }

    public record PayoutRequest(String payoutId, long amountMinor, String currency, String destinationType, String destinationAccount) {
    }

    public record PayoutResult(String payoutId, String status, String bankRef, String reason) {
    }

    public record StatementLine(String bankRef, LocalDate valueDate, String currency, long amountMinor, String description) {
    }

    public record FxQuote(String base, String quote, BigDecimal midRate) {
    }

    public record SimulatorConfig(boolean declineAll, long latencyMs) {
    }
}
