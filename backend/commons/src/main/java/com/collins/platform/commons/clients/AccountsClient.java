package com.collins.platform.commons.clients;

import com.collins.platform.commons.http.ServiceClient;
import com.collins.platform.commons.http.ServiceClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** REST client for accounts-service (merchants, individuals and their wallets). */
@Component
@ConditionalOnProperty(name = "platform.services.accounts-url")
public class AccountsClient {

    public record Merchant(String merchantId, String entityId, String name, String country, String settlementCurrency,
                           int reservePercentBp, int reserveDays, String status) {
        public String payableAccount() {
            return "2000:" + merchantId;
        }
    }

    public record Wallet(String walletId, String individualId, String entityId, String currency, String kind,
                         String ledgerAccount, Long availableMinor) {
    }

    private final ServiceClient client;

    public AccountsClient(ServiceClientFactory factory, @Value("${platform.services.accounts-url}") String baseUrl) {
        this.client = factory.create("accounts-service", baseUrl);
    }

    public Merchant merchant(String merchantId) {
        return client.get("/v1/merchants/" + merchantId, Merchant.class);
    }

    public Wallet wallet(String walletId) {
        return client.get("/v1/wallets/" + walletId, Wallet.class);
    }

    /** All wallets of an individual with their available balances, used by collections for recovery. */
    public List<Wallet> wallets(String individualId) {
        return client.getList("/v1/individuals/" + individualId + "/wallets", Wallet.class);
    }
}
