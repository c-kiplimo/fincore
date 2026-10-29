package com.collins.platform.pricing.api;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.pricing.api.PricingDtos.AccrueRequest;
import com.collins.platform.pricing.api.PricingDtos.AccrueResult;
import com.collins.platform.pricing.api.PricingDtos.FxQuoteRequest;
import com.collins.platform.pricing.api.PricingDtos.FxRateRequest;
import com.collins.platform.pricing.api.PricingDtos.ScheduleRequest;
import com.collins.platform.pricing.api.PricingDtos.TaxRuleRequest;
import com.collins.platform.pricing.application.QuoteService;
import com.collins.platform.pricing.application.RulesService;
import com.collins.platform.pricing.domain.DayCountConvention;
import com.collins.platform.pricing.domain.QuoteModel;
import com.collins.platform.pricing.infrastructure.PricingRepository;
import com.collins.platform.pricing.infrastructure.PricingRepository.FeePlan;
import com.collins.platform.pricing.infrastructure.PricingRepository.FxRate;
import com.collins.platform.pricing.infrastructure.PricingRepository.TaxRule;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Fee plans, tax rules, FX rates and interest maths. */
@RestController
public class RulesController {

    private final RulesService rules;
    private final QuoteService quotes;
    private final PricingRepository repo;

    public RulesController(RulesService rules, QuoteService quotes, PricingRepository repo) {
        this.rules = rules;
        this.quotes = quotes;
        this.repo = repo;
    }

    @GetMapping("/v1/fee-plans")
    public ApiResponse<List<FeePlan>> feePlans(@RequestParam String entityId) {
        return ApiResponse.ok(repo.feePlans(entityId));
    }

    @GetMapping("/v1/tax-rules")
    public ApiResponse<List<TaxRule>> taxRules(@RequestParam(required = false) String jurisdiction) {
        return ApiResponse.ok(rules.taxRules(jurisdiction));
    }

    @PostMapping("/v1/tax-rules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TaxRule> newTaxRuleVersion(@Valid @RequestBody ApiRequest<TaxRuleRequest> request) {
        TaxRuleRequest p = request.payload();
        return ApiResponse.ok(rules.newVersion(new TaxRule(0, p.ruleCode(), 0, p.jurisdiction(), p.product(), p.taxType(), "FEE",
                p.rate(), p.ledgerAccount(), p.reverseChargeB2b(), p.validFrom(), null, p.example(), p.description(), p.sourceUrl())));
    }

    @GetMapping("/v1/fx-rates")
    public ApiResponse<List<FxRate>> fxRates() {
        return ApiResponse.ok(rules.fxRates());
    }

    @PutMapping("/v1/fx-rates")
    public ApiResponse<FxRate> setFxRate(@Valid @RequestBody ApiRequest<FxRateRequest> request) {
        FxRateRequest p = request.payload();
        return ApiResponse.ok(rules.setFxRate(p.base(), p.quote(), p.midRate(), p.spreadBp()));
    }

    @PostMapping("/v1/fx-rates/convert")
    public ApiResponse<QuoteModel.Fx> convert(@Valid @RequestBody ApiRequest<FxQuoteRequest> request) {
        FxQuoteRequest p = request.payload();
        return ApiResponse.ok(quotes.fx(p.amountMinor(), p.from(), p.to()));
    }

    @PostMapping("/v1/interest/schedule")
    public ApiResponse<RulesService.Schedule> schedule(@Valid @RequestBody ApiRequest<ScheduleRequest> request) {
        ScheduleRequest p = request.payload();
        return ApiResponse.ok(rules.schedule(p.principalMinor(), p.currency(), p.annualRateBp(), p.months(), p.firstDueDate(),
                p.jurisdiction(), p.product() == null ? "INSTALMENT_LOAN" : p.product()));
    }

    @PostMapping("/v1/interest/accrue")
    public ApiResponse<AccrueResult> accrue(@Valid @RequestBody ApiRequest<AccrueRequest> request) {
        AccrueRequest p = request.payload();
        DayCountConvention dayCount = rules.dayCount(p.jurisdiction(), p.product() == null ? "INSTALMENT_LOAN" : p.product(), p.dayCount());
        long interest = rules.accrue(p.principalMinor(), p.annualRateBp(), p.from(), p.to(), dayCount);
        return ApiResponse.ok(new AccrueResult(p.principalMinor(), p.annualRateBp(), p.from(), p.to(), dayCount.name(), interest));
    }
}
