package com.collins.platform.pricing.application;

import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.commons.money.CurrencyUnits;
import com.collins.platform.pricing.domain.DayCountConvention;
import com.collins.platform.pricing.domain.InterestMath;
import com.collins.platform.pricing.infrastructure.PricingRepository;
import com.collins.platform.pricing.infrastructure.PricingRepository.InterestRule;
import com.collins.platform.pricing.infrastructure.PricingRepository.TaxRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Tax-rule versions, FX rates and interest calculations. */
@Service
public class RulesService {

    public record Schedule(long principalMinor, String currency, int annualRateBp, int months, String dayCount,
                           long totalInterestMinor, long totalRepayableMinor, List<InterestMath.ScheduleRow> rows) {
    }

    private final PricingRepository repo;

    public RulesService(PricingRepository repo) {
        this.repo = repo;
    }

    public List<TaxRule> taxRules(String jurisdiction) {
        return repo.allTaxRules(jurisdiction);
    }

    /**
     * Adds a new version of a tax rule. The previous version is closed the day before, so versions never overlap
     * and an old transaction can always be recalculated under the rule that applied on its date.
     */
    @Transactional
    public TaxRule newVersion(TaxRule draft) {
        Optional<TaxRule> latest = repo.latestVersion(draft.ruleCode());
        int version = latest.map(r -> r.version() + 1).orElse(1);
        latest.ifPresent(r -> {
            if (!draft.validFrom().isAfter(r.validFrom())) {
                throw new BusinessRuleException("OVERLAPPING_RULE", "A new version must start after " + r.validFrom());
            }
            repo.closeRule(r.id(), draft.validFrom().minusDays(1));
        });
        TaxRule toInsert = new TaxRule(0, draft.ruleCode(), version, draft.jurisdiction(), draft.product(), draft.taxType(),
                draft.base() == null ? "FEE" : draft.base(), draft.rate(), draft.ledgerAccount(), draft.reverseChargeB2b(),
                draft.validFrom(), null, draft.example(), draft.description(), draft.sourceUrl());
        repo.insertRule(toInsert);
        return repo.latestVersion(draft.ruleCode()).orElseThrow();
    }

    public List<PricingRepository.FxRate> fxRates() {
        return repo.fxRates();
    }

    @Transactional
    public PricingRepository.FxRate setFxRate(String base, String quote, BigDecimal mid, int spreadBp) {
        if (mid.signum() <= 0) {
            throw new BusinessRuleException("INVALID_RATE", "Rate must be positive");
        }
        repo.upsertFxRate(base, quote, mid, spreadBp);
        return repo.fxRate(base, quote).orElseThrow();
    }

    public DayCountConvention dayCount(String jurisdiction, String product, String requested) {
        if (requested != null && !requested.isBlank()) {
            return DayCountConvention.valueOf(requested);
        }
        return repo.interestRule(jurisdiction, product).map(r -> DayCountConvention.valueOf(r.dayCount()))
                .orElse(DayCountConvention.ACT_365);
    }

    public Optional<InterestRule> interestRule(String jurisdiction, String product) {
        return repo.interestRule(jurisdiction, product);
    }

    public Schedule schedule(long principalMinor, String currency, int annualRateBp, int months, LocalDate firstDueDate,
                             String jurisdiction, String product) {
        if (months <= 0 || principalMinor <= 0 || !CurrencyUnits.supported(currency)) {
            throw new BusinessRuleException("INVALID_LOAN_TERMS", "Principal, currency and months must be valid");
        }
        checkCap(jurisdiction, product, annualRateBp);
        List<InterestMath.ScheduleRow> rows = InterestMath.annuity(principalMinor, annualRateBp, months, firstDueDate);
        long interest = InterestMath.totalInterest(rows);
        return new Schedule(principalMinor, currency, annualRateBp, months, "MONTHLY_ANNUITY", interest, principalMinor + interest, rows);
    }

    public long accrue(long principalMinor, int annualRateBp, LocalDate from, LocalDate to, DayCountConvention dayCount) {
        return InterestMath.accrue(principalMinor, annualRateBp, from, to, dayCount);
    }

    private void checkCap(String jurisdiction, String product, int annualRateBp) {
        if (jurisdiction == null) {
            return;
        }
        repo.interestRule(jurisdiction, product).ifPresent(rule -> {
            if (rule.maxAnnualRateBp() != null && annualRateBp > rule.maxAnnualRateBp()) {
                throw new BusinessRuleException("RATE_ABOVE_CAP", "Rate is above the legal cap for " + jurisdiction);
            }
        });
    }
}
