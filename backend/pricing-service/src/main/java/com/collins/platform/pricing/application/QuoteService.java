package com.collins.platform.pricing.application;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.exception.ConflictException;
import com.collins.platform.commons.exception.NotFoundException;
import com.collins.platform.commons.jdbc.Ids;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.pricing.domain.PricingRules;
import com.collins.platform.pricing.domain.QuoteModel;
import com.collins.platform.pricing.infrastructure.PricingRepository;
import com.collins.platform.pricing.infrastructure.PricingRepository.EntityRef;
import com.collins.platform.pricing.infrastructure.PricingRepository.FeePlan;
import com.collins.platform.pricing.infrastructure.PricingRepository.FxRate;
import com.collins.platform.pricing.infrastructure.PricingRepository.TaxRule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Prices a transaction before it runs (Stripe-style quote). The caller shows the quote to the customer,
 * then sends the quote id when executing; {@link #consume} makes sure it is used once and before expiry.
 */
@Service
public class QuoteService {

    public record QuoteRequest(String entityId, String product, long amountMinor, String currency, String targetCurrency,
                               String customerCountry, String customerType, LocalDate transactionDate) {
    }

    private final PricingRepository repo;
    private final ObjectMapper mapper;
    private final long ttlSeconds;

    public QuoteService(PricingRepository repo, ObjectMapper mapper, @Value("${pricing.quote-ttl-seconds:120}") long ttlSeconds) {
        this.repo = repo;
        this.mapper = mapper;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public QuoteModel create(QuoteRequest r) {
        if (r.amountMinor() <= 0) {
            throw new BusinessRuleException("INVALID_AMOUNT", "amountMinor must be positive");
        }
        if (!CurrencyUnits.supported(r.currency())) {
            throw new BusinessRuleException("UNSUPPORTED_CURRENCY", "Unsupported currency " + r.currency());
        }
        EntityRef entity = repo.entity(r.entityId()).orElseThrow(() -> new NotFoundException("Entity", r.entityId()));
        LocalDate on = r.transactionDate() == null ? LocalDate.now() : r.transactionDate();
        FeePlan plan = repo.feePlan(entity.entityId(), r.product(), on)
                .orElseThrow(() -> new BusinessRuleException("NO_FEE_PLAN", "No fee plan for " + r.product() + " in " + entity.entityId()));

        long feeAmount = PricingRules.fee(r.amountMinor(), plan.percentBp(), plan.fixedMinor(), plan.minMinor(), plan.maxMinor());
        List<QuoteModel.Fee> fees = feeAmount > 0 ? List.of(new QuoteModel.Fee(r.product().toLowerCase() + "_fee", feeAmount)) : List.of();

        String jurisdiction = entity.country();
        boolean business = "BUSINESS".equalsIgnoreCase(r.customerType());
        boolean crossBorder = r.customerCountry() != null && !r.customerCountry().equalsIgnoreCase(jurisdiction);
        List<QuoteModel.Tax> taxes = new ArrayList<>();
        for (TaxRule rule : repo.taxRules(jurisdiction, r.product(), on)) {
            boolean reverseCharge = rule.reverseChargeB2b() && business && crossBorder;
            long tax = reverseCharge ? 0 : PricingRules.tax(feeAmount, rule.rate());
            taxes.add(new QuoteModel.Tax(rule.taxType(), rule.jurisdiction(), rule.ruleCode(), rule.version(), rule.rate(),
                    feeAmount, tax, rule.ledgerAccount(), reverseCharge, rule.example()));
        }
        long totalTaxes = taxes.stream().mapToLong(QuoteModel.Tax::amountMinor).sum();

        QuoteModel.Fx fx = null;
        if (r.targetCurrency() != null && !r.targetCurrency().equals(r.currency())) {
            fx = fx(r.amountMinor(), r.currency(), r.targetCurrency());
        }

        boolean payerPays = "PAYER".equals(plan.feePayer());
        long totalDebited = payerPays ? r.amountMinor() + feeAmount + totalTaxes : r.amountMinor();
        long netToRecipient = payerPays ? r.amountMinor() : r.amountMinor() - feeAmount - totalTaxes;
        if (netToRecipient < 0) {
            throw new BusinessRuleException("AMOUNT_TOO_SMALL", "The amount does not cover fees and taxes");
        }

        String id = Ids.newId("qt");
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        QuoteModel quote = new QuoteModel(id, entity.entityId(), jurisdiction, r.product(), r.currency(), r.amountMinor(),
                plan.feePayer(), fees, taxes, fx, feeAmount, totalTaxes, totalDebited, netToRecipient, "OPEN", expiresAt);
        repo.insertQuote(id, entity.entityId(), r.product(), json(quote), expiresAt);
        return quote;
    }

    public QuoteModel get(String quoteId) {
        PricingRepository.StoredQuote stored = repo.quote(quoteId).orElseThrow(() -> new NotFoundException("Quote", quoteId));
        return read(stored.quoteJson()).withStatus(stored.status());
    }

    /** Uses a quote exactly once. Re-consuming with the same reference is idempotent. */
    @Transactional
    public QuoteModel consume(String quoteId, String reference) {
        PricingRepository.StoredQuote stored = repo.quote(quoteId).orElseThrow(() -> new NotFoundException("Quote", quoteId));
        if (repo.consume(quoteId, reference)) {
            return read(stored.quoteJson()).withStatus("USED");
        }
        if ("USED".equals(stored.status()) && reference != null && reference.equals(stored.usedBy())) {
            return read(stored.quoteJson()).withStatus("USED");
        }
        if ("USED".equals(stored.status())) {
            throw new ConflictException("QUOTE_ALREADY_USED", "Quote " + quoteId + " was already used");
        }
        throw new BusinessRuleException("QUOTE_EXPIRED", "Quote " + quoteId + " has expired; request a new quote");
    }

    /** Mid-market rate (direct, or inverted from the reverse pair) and the customer rate after the spread. */
    public QuoteModel.Fx fx(long amountMinor, String from, String to) {
        FxRate direct = repo.fxRate(from, to).orElse(null);
        BigDecimal mid;
        int spread;
        if (direct != null) {
            mid = direct.midRate();
            spread = direct.spreadBp();
        } else {
            FxRate inverse = repo.fxRate(to, from)
                    .orElseThrow(() -> new BusinessRuleException("NO_FX_RATE", "No FX rate for " + from + "/" + to));
            mid = BigDecimal.ONE.divide(inverse.midRate(), MathContext.DECIMAL64).setScale(10, RoundingMode.HALF_EVEN);
            spread = inverse.spreadBp();
        }
        BigDecimal customerRate = PricingRules.customerRate(mid, spread);
        long midConverted = CurrencyUnits.convert(amountMinor, from, mid, to);
        long converted = CurrencyUnits.convert(amountMinor, from, customerRate, to);
        return new QuoteModel.Fx(from, to, mid, customerRate, spread, midConverted, converted, midConverted - converted);
    }

    private String json(QuoteModel quote) {
        try {
            return mapper.writeValueAsString(quote);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private QuoteModel read(String json) {
        try {
            return mapper.readValue(json, QuoteModel.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
