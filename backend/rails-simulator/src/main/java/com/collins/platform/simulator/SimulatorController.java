package com.collins.platform.simulator;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.simulator.SimulatorModels.AuthorizeRequest;
import com.collins.platform.simulator.SimulatorModels.AuthorizeResult;
import com.collins.platform.simulator.SimulatorModels.FxQuote;
import com.collins.platform.simulator.SimulatorModels.NetworkEvent;
import com.collins.platform.simulator.SimulatorModels.NetworkRefRequest;
import com.collins.platform.simulator.SimulatorModels.PayoutRequest;
import com.collins.platform.simulator.SimulatorModels.PayoutResult;
import com.collins.platform.simulator.SimulatorModels.SettlementBatch;
import com.collins.platform.simulator.SimulatorModels.SettlementRequest;
import com.collins.platform.simulator.SimulatorModels.SimulatorConfig;
import com.collins.platform.simulator.SimulatorModels.StatementLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test magic values:
 * <ul>
 *   <li>Payment amount ending in 13 minor units (e.g. 100013) is declined.</li>
 *   <li>Payout destination ending in 0000 fails; ending in 9999 times out first, then succeeds on status check.</li>
 * </ul>
 */
@RestController
public class SimulatorController {

    private final Map<String, AuthorizeRequest> authorizations = new ConcurrentHashMap<>();
    private final List<NetworkEvent> unsettled = new CopyOnWriteArrayList<>();
    private final Map<String, PayoutResult> payouts = new ConcurrentHashMap<>();
    private final List<StatementLine> bankStatement = new CopyOnWriteArrayList<>();
    private volatile SimulatorConfig config = new SimulatorConfig(false, 0);
    private final int networkFeeBp;

    public SimulatorController(@Value("${simulator.network-fee-bp:150}") int networkFeeBp) {
        this.networkFeeBp = networkFeeBp;
    }

    // ---------------- card network / mobile money ----------------

    @PostMapping("/sim/card/authorizations")
    public ApiResponse<AuthorizeResult> authorize(@RequestBody ApiRequest<AuthorizeRequest> request) {
        delay();
        AuthorizeRequest p = request.payload();
        String ref = "nw_" + UUID.randomUUID().toString().substring(0, 12);
        if (config.declineAll() || p.amountMinor() % 100 == 13) {
            return ApiResponse.ok(new AuthorizeResult(ref, "DECLINED", "do_not_honour"));
        }
        authorizations.put(ref, p);
        return ApiResponse.ok(new AuthorizeResult(ref, "APPROVED", null));
    }

    @PostMapping("/sim/card/captures")
    public ApiResponse<NetworkEvent> capture(@RequestBody ApiRequest<NetworkRefRequest> request) {
        delay();
        AuthorizeRequest auth = auth(request.payload().networkRef());
        long fee = BigDecimal.valueOf(request.payload().amountMinor()).multiply(BigDecimal.valueOf(networkFeeBp))
                .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_EVEN).longValueExact();
        NetworkEvent event = new NetworkEvent(request.payload().networkRef(), auth.paymentId(), "CAPTURE",
                request.payload().amountMinor(), fee, auth.currency());
        unsettled.add(event);
        return ApiResponse.ok(event);
    }

    @PostMapping("/sim/card/refunds")
    public ApiResponse<NetworkEvent> refund(@RequestBody ApiRequest<NetworkRefRequest> request) {
        AuthorizeRequest auth = auth(request.payload().networkRef());
        NetworkEvent event = new NetworkEvent(request.payload().networkRef(), auth.paymentId(), "REFUND",
                -request.payload().amountMinor(), 0, auth.currency());
        unsettled.add(event);
        return ApiResponse.ok(event);
    }

    @PostMapping("/sim/card/chargebacks")
    public ApiResponse<NetworkEvent> chargeback(@RequestBody ApiRequest<NetworkRefRequest> request) {
        AuthorizeRequest auth = auth(request.payload().networkRef());
        NetworkEvent event = new NetworkEvent(request.payload().networkRef(), auth.paymentId(), "CHARGEBACK",
                -request.payload().amountMinor(), 0, auth.currency());
        unsettled.add(event);
        return ApiResponse.ok(event);
    }

    @PostMapping("/sim/card/chargeback-reversals")
    public ApiResponse<NetworkEvent> chargebackReversal(@RequestBody ApiRequest<NetworkRefRequest> request) {
        AuthorizeRequest auth = auth(request.payload().networkRef());
        NetworkEvent event = new NetworkEvent(request.payload().networkRef(), auth.paymentId(), "CHARGEBACK_REVERSAL",
                request.payload().amountMinor(), 0, auth.currency());
        unsettled.add(event);
        return ApiResponse.ok(event);
    }

    /** Pays out everything not yet settled in a currency as one batch, net of network fees, and credits the bank. */
    @PostMapping("/sim/card/settlements")
    public synchronized ApiResponse<SettlementBatch> settle(@RequestBody ApiRequest<SettlementRequest> request) {
        String currency = request.payload().currency();
        LocalDate date = request.payload().date() == null ? LocalDate.now() : request.payload().date();
        List<NetworkEvent> lines = new ArrayList<>();
        for (NetworkEvent e : unsettled) {
            if (e.currency().equals(currency)) {
                lines.add(e);
            }
        }
        unsettled.removeAll(lines);
        long gross = lines.stream().mapToLong(NetworkEvent::grossMinor).sum();
        long fees = lines.stream().mapToLong(NetworkEvent::feeMinor).sum();
        String batchId = "stl_" + UUID.randomUUID().toString().substring(0, 12);
        if (gross - fees != 0) {
            bankStatement.add(new StatementLine(batchId, date, currency, gross - fees, "Card network settlement " + batchId));
        }
        return ApiResponse.ok(new SettlementBatch(batchId, currency, date, lines, gross, fees, gross - fees));
    }

    // ---------------- bank ----------------

    @PostMapping("/sim/bank/payouts")
    public ApiResponse<PayoutResult> payout(@RequestBody ApiRequest<PayoutRequest> request) {
        delay();
        PayoutRequest p = request.payload();
        PayoutResult existing = payouts.get(p.payoutId());
        if (existing != null && !"TIMEOUT".equals(existing.status())) {
            return ApiResponse.ok(existing);
        }
        String account = p.destinationAccount() == null ? "" : p.destinationAccount();
        PayoutResult result;
        if (account.endsWith("0000")) {
            result = new PayoutResult(p.payoutId(), "FAILED", null, "account_closed");
        } else if (account.endsWith("9999") && existing == null) {
            payouts.put(p.payoutId(), new PayoutResult(p.payoutId(), "TIMEOUT", null, "bank_timeout"));
            bankStatement.add(new StatementLine(p.payoutId(), LocalDate.now(), p.currency(), -p.amountMinor(), "Payout " + p.payoutId()));
            return ApiResponse.ok(new PayoutResult(p.payoutId(), "TIMEOUT", null, "bank_timeout"));
        } else {
            result = new PayoutResult(p.payoutId(), "PAID", "bnk_" + UUID.randomUUID().toString().substring(0, 10), null);
            bankStatement.add(new StatementLine(p.payoutId(), LocalDate.now(), p.currency(), -p.amountMinor(), "Payout " + p.payoutId()));
        }
        payouts.put(p.payoutId(), result);
        return ApiResponse.ok(result);
    }

    /** Status check; a timed-out payout resolves to PAID here (the bank did receive it). */
    @GetMapping("/sim/bank/payouts/{payoutId}")
    public ApiResponse<PayoutResult> payoutStatus(@PathVariable String payoutId) {
        PayoutResult result = payouts.get(payoutId);
        if (result == null) {
            throw new NotFoundException("Payout", payoutId);
        }
        if ("TIMEOUT".equals(result.status())) {
            result = new PayoutResult(payoutId, "PAID", "bnk_" + UUID.randomUUID().toString().substring(0, 10), null);
            payouts.put(payoutId, result);
        }
        return ApiResponse.ok(result);
    }

    @GetMapping("/sim/bank/statements")
    public ApiResponse<List<StatementLine>> statement(@RequestParam String currency,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(bankStatement.stream().filter(l -> l.currency().equals(currency) && l.valueDate().equals(date)).toList());
    }

    /** Test helper: add an unexplained deposit to the bank statement (reconciliation should flag it). */
    @PostMapping("/sim/bank/unknown-deposits")
    public ApiResponse<StatementLine> unknownDeposit(@RequestBody ApiRequest<StatementLine> request) {
        StatementLine p = request.payload();
        StatementLine line = new StatementLine("unk_" + UUID.randomUUID().toString().substring(0, 8),
                p.valueDate() == null ? LocalDate.now() : p.valueDate(), p.currency(), p.amountMinor(), "Unknown deposit");
        bankStatement.add(line);
        return ApiResponse.ok(line);
    }

    // ---------------- FX feed and config ----------------

    @GetMapping("/sim/fx/rates")
    public ApiResponse<List<FxQuote>> rates() {
        return ApiResponse.ok(List.of(new FxQuote("USD", "KES", new BigDecimal("130.00")),
                new FxQuote("GBP", "KES", new BigDecimal("165.00")), new FxQuote("USD", "GBP", new BigDecimal("0.79"))));
    }

    @PostMapping("/sim/config")
    public ApiResponse<SimulatorConfig> configure(@RequestBody ApiRequest<SimulatorConfig> request) {
        config = request.payload();
        return ApiResponse.ok(config);
    }

    private AuthorizeRequest auth(String networkRef) {
        AuthorizeRequest auth = authorizations.get(networkRef);
        if (auth == null) {
            throw new NotFoundException("Authorization", networkRef);
        }
        return auth;
    }

    private void delay() {
        if (config.latencyMs() > 0) {
            try {
                Thread.sleep(config.latencyMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
