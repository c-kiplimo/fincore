package com.collins.platform.commons.clients;

import com.collins.platform.commons.http.ServiceClient;
import com.collins.platform.commons.http.ServiceClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** REST client for pricing-service. Every money-moving service gets a quote first, then consumes it. */
@Component
@ConditionalOnProperty(name = "platform.services.pricing-url")
public class PricingClient {

    private final ServiceClient client;

    public PricingClient(ServiceClientFactory factory, @Value("${platform.services.pricing-url}") String baseUrl) {
        this.client = factory.create("pricing-service", baseUrl);
    }

    public Quote quote(String entityId, String product, long amountMinor, String currency) {
        return quote(entityId, product, amountMinor, currency, null, null, null);
    }

    public Quote quote(String entityId, String product, long amountMinor, String currency, String targetCurrency,
                       String customerCountry, String customerType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("entityId", entityId);
        payload.put("product", product);
        payload.put("amountMinor", amountMinor);
        payload.put("currency", currency);
        payload.put("targetCurrency", targetCurrency);
        payload.put("customerCountry", customerCountry);
        payload.put("customerType", customerType);
        return client.post("/v1/quotes", payload, Quote.class);
    }

    /** Uses the quote once; the reference makes a retry of the same operation idempotent. */
    public Quote consume(String quoteId, String reference) {
        return client.post("/v1/quotes/" + quoteId + "/consume", Map.of("reference", reference), Quote.class);
    }

    public Quote quoteAndConsume(String entityId, String product, long amountMinor, String currency, String reference) {
        Quote quote = quote(entityId, product, amountMinor, currency);
        return consume(quote.quoteId(), reference);
    }

    public Quote.Fx fx(long amountMinor, String from, String to) {
        return client.post("/v1/fx-rates/convert", Map.of("amountMinor", amountMinor, "from", from, "to", to), Quote.Fx.class);
    }

    public Schedule schedule(long principalMinor, String currency, int annualRateBp, int months, java.time.LocalDate firstDueDate,
                             String jurisdiction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("principalMinor", principalMinor);
        payload.put("currency", currency);
        payload.put("annualRateBp", annualRateBp);
        payload.put("months", months);
        payload.put("firstDueDate", firstDueDate.toString());
        payload.put("jurisdiction", jurisdiction);
        return client.post("/v1/interest/schedule", payload, Schedule.class);
    }

    public Accrual accrue(long principalMinor, int annualRateBp, java.time.LocalDate from, java.time.LocalDate to, String jurisdiction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("principalMinor", principalMinor);
        payload.put("annualRateBp", annualRateBp);
        payload.put("from", from.toString());
        payload.put("to", to.toString());
        payload.put("jurisdiction", jurisdiction);
        return client.post("/v1/interest/accrue", payload, Accrual.class);
    }

    public record Schedule(long principalMinor, String currency, int annualRateBp, int months, String dayCount,
                           long totalInterestMinor, long totalRepayableMinor, java.util.List<Row> rows) {
        public record Row(int number, java.time.LocalDate dueDate, long openingPrincipalMinor, long interestMinor,
                          long principalMinor, long instalmentMinor, long closingPrincipalMinor) {
        }
    }

    public record Accrual(long principalMinor, int annualRateBp, java.time.LocalDate from, java.time.LocalDate to,
                          String dayCount, long interestMinor) {
    }
}
