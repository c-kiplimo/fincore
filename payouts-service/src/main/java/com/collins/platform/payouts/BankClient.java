package com.collins.platform.payouts;

import com.collins.platform.commons.http.ServiceClient;
import com.collins.platform.commons.http.ServiceClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Anti-corruption layer for the bank's payout API. */
@Component
public class BankClient {

    public record BankResult(String payoutId, String status, String bankRef, String reason) {
    }

    private final ServiceClient client;

    public BankClient(ServiceClientFactory factory, @Value("${platform.services.simulator-url}") String baseUrl) {
        this.client = factory.create("bank", baseUrl);
    }

    public BankResult send(Payout p) {
        return client.post("/sim/bank/payouts", Map.of("payoutId", p.id(), "amountMinor", p.amountMinor(), "currency", p.currency(),
                "destinationType", p.destinationType(), "destinationAccount", p.destinationAccount()), BankResult.class);
    }

    public BankResult status(String payoutId) {
        return client.get("/sim/bank/payouts/" + payoutId, BankResult.class);
    }
}
