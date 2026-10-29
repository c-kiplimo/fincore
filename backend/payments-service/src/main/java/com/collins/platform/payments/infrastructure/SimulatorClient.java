package com.collins.platform.payments.infrastructure;

import com.collins.platform.commons.http.ServiceClient;
import com.collins.platform.commons.http.ServiceClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Anti-corruption layer: the only class that knows the card network's API. */
@Component
public class SimulatorClient {

    public record Authorization(String networkRef, String status, String reason) {
    }

    public record NetworkEvent(String networkRef, String paymentId, String type, long grossMinor, long feeMinor, String currency) {
    }

    public record SettlementBatch(String batchId, String currency, LocalDate date, List<NetworkEvent> lines,
                                  long grossMinor, long feesMinor, long netMinor) {
    }

    private final ServiceClient client;

    public SimulatorClient(ServiceClientFactory factory, @Value("${platform.services.simulator-url}") String baseUrl) {
        this.client = factory.create("rails-simulator", baseUrl);
    }

    public Authorization authorize(String paymentId, long amountMinor, String currency, String method, String instrument) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", paymentId);
        payload.put("amountMinor", amountMinor);
        payload.put("currency", currency);
        payload.put("method", method);
        payload.put("instrument", instrument);
        return client.post("/sim/card/authorizations", payload, Authorization.class);
    }

    public NetworkEvent capture(String networkRef, long amountMinor) {
        return client.post("/sim/card/captures", Map.of("networkRef", networkRef, "amountMinor", amountMinor), NetworkEvent.class);
    }

    public NetworkEvent refund(String networkRef, long amountMinor) {
        return client.post("/sim/card/refunds", Map.of("networkRef", networkRef, "amountMinor", amountMinor), NetworkEvent.class);
    }

    public NetworkEvent chargeback(String networkRef, long amountMinor, String reason) {
        return client.post("/sim/card/chargebacks", Map.of("networkRef", networkRef, "amountMinor", amountMinor,
                "reason", reason == null ? "" : reason), NetworkEvent.class);
    }

    public NetworkEvent chargebackReversal(String networkRef, long amountMinor) {
        return client.post("/sim/card/chargeback-reversals", Map.of("networkRef", networkRef, "amountMinor", amountMinor),
                NetworkEvent.class);
    }

    public SettlementBatch settle(String currency, LocalDate date) {
        return client.post("/sim/card/settlements", Map.of("currency", currency, "date", date.toString()), SettlementBatch.class);
    }
}
