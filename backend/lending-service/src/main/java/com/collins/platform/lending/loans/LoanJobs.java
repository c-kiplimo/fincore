package com.collins.platform.lending.loans;

import com.collins.platform.lending.loans.LoanModels.Loan;
import com.collins.platform.lending.loans.LoanModels.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The lending day: accrue interest, collect what is due, count days past due, charge late fees, restage and provision.
 * Runs on a cron and can be triggered for any date (useful to simulate months in tests).
 */
@Component
public class LoanJobs {

    private static final Logger log = LoggerFactory.getLogger(LoanJobs.class);

    public record DailyResult(LocalDate asOf, int loansProcessed, List<String> failures) {
    }

    private final LoanService loans;
    private final boolean enabled;

    public LoanJobs(LoanService loans, @Value("${lending.jobs-enabled:true}") boolean enabled) {
        this.loans = loans;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${lending.daily-cron:0 30 1 * * *}")
    public void scheduled() {
        if (enabled) {
            DailyResult result = runDaily(LocalDate.now());
            log.info("Lending daily run: {} loans, {} failures", result.loansProcessed(), result.failures().size());
        }
    }

    public DailyResult runDaily(LocalDate asOf) {
        int processed = 0;
        List<String> failures = new ArrayList<>();
        List<Loan> all = new ArrayList<>(loans.active("INSTALMENT_LOAN"));
        all.addAll(loans.active("MERCHANT_FINANCING"));
        for (Loan loan : all) {
            try {
                Product product = loans.product(loan.productCode());
                loans.daily(loan.id(), asOf, product.graceDays(), product.lateFeeMinor());
                processed++;
            } catch (RuntimeException ex) {
                log.warn("Daily run failed for loan {}: {}", loan.id(), ex.getMessage());
                failures.add(loan.id() + ": " + ex.getMessage());
            }
        }
        return new DailyResult(asOf, processed, failures);
    }
}
