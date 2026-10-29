package com.collins.platform.lending.loans;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.idempotency.IdempotencyService;
import com.collins.platform.lending.loans.LoanModels.Loan;
import com.collins.platform.lending.loans.LoanModels.Product;
import com.collins.platform.lending.loans.LoanModels.Repayment;
import com.collins.platform.lending.loans.LoanModels.ScheduleRow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class LoanController {

    public record ApplyRequest(@NotBlank String productCode, @NotBlank @Pattern(regexp = "MERCHANT|INDIVIDUAL") String borrowerType,
                               @NotBlank String borrowerId, String walletId, @Positive long principalMinor) {
    }

    public record RepayRequest(@Positive long amountMinor, String sourceAccount) {
    }

    public record PayoffRequest(String sourceAccount) {
    }

    public record WriteOffRequest(String reason) {
    }

    public record RestructureRequest(@Positive int extraMonths) {
    }

    public record DailyRequest(LocalDate asOf) {
    }

    private final LoanService loans;
    private final LoanJobs jobs;
    private final IdempotencyService idempotency;

    public LoanController(LoanService loans, LoanJobs jobs, IdempotencyService idempotency) {
        this.loans = loans;
        this.jobs = jobs;
        this.idempotency = idempotency;
    }

    @GetMapping("/v1/loan-products")
    public ApiResponse<List<Product>> products() {
        return ApiResponse.ok(loans.products());
    }

    @PostMapping("/v1/loans")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Loan> apply(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ApiRequest<ApplyRequest> request) {
        ApplyRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("loan-apply", key, p, Loan.class,
                () -> loans.apply(p.productCode(), p.borrowerType(), p.borrowerId(), p.walletId(), p.principalMinor())));
    }

    @GetMapping("/v1/loans/{loanId}")
    public ApiResponse<Loan> get(@PathVariable String loanId) {
        return ApiResponse.ok(loans.get(loanId));
    }

    @GetMapping("/v1/loans")
    public ApiResponse<List<Loan>> byBorrower(@RequestParam String borrowerId) {
        return ApiResponse.ok(loans.byBorrower(borrowerId));
    }

    @PostMapping("/v1/loans/{loanId}/disburse")
    public ApiResponse<Loan> disburse(@PathVariable String loanId) {
        return ApiResponse.ok(loans.disburse(loanId));
    }

    @GetMapping("/v1/loans/{loanId}/schedule")
    public ApiResponse<List<ScheduleRow>> schedule(@PathVariable String loanId) {
        return ApiResponse.ok(loans.schedule(loanId));
    }

    @GetMapping("/v1/loans/{loanId}/repayments")
    public ApiResponse<List<Repayment>> repayments(@PathVariable String loanId) {
        return ApiResponse.ok(loans.repayments(loanId));
    }

    @PostMapping("/v1/loans/{loanId}/repayments")
    public ApiResponse<Repayment> repay(@PathVariable String loanId, @RequestHeader("Idempotency-Key") String key,
                                        @Valid @RequestBody ApiRequest<RepayRequest> request) {
        RepayRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("repay:" + loanId, key, p, Repayment.class,
                () -> loans.repay(loanId, p.amountMinor(), p.sourceAccount(), "repayment:" + loanId + ":" + key)));
    }

    @GetMapping("/v1/loans/{loanId}/payoff-quote")
    public ApiResponse<LoanService.PayoffQuote> payoffQuote(@PathVariable String loanId) {
        return ApiResponse.ok(loans.payoffQuote(loanId));
    }

    @PostMapping("/v1/loans/{loanId}/payoff")
    public ApiResponse<Repayment> payoff(@PathVariable String loanId, @RequestBody(required = false) ApiRequest<PayoffRequest> request) {
        String source = request == null || request.payload() == null ? null : request.payload().sourceAccount();
        return ApiResponse.ok(loans.repayInFull(loanId, source));
    }

    @PostMapping("/v1/loans/{loanId}/restructure")
    public ApiResponse<Loan> restructure(@PathVariable String loanId, @Valid @RequestBody ApiRequest<RestructureRequest> request) {
        return ApiResponse.ok(loans.restructure(loanId, request.payload().extraMonths()));
    }

    @PostMapping("/v1/loans/{loanId}/write-off")
    public ApiResponse<Loan> writeOff(@PathVariable String loanId, @RequestBody(required = false) ApiRequest<WriteOffRequest> request) {
        return ApiResponse.ok(loans.writeOff(loanId, request == null || request.payload() == null ? null : request.payload().reason()));
    }

    /** Runs the lending day for any date, so you can simulate months passing in tests. */
    @PostMapping("/v1/lending-jobs/daily")
    public ApiResponse<LoanJobs.DailyResult> daily(@RequestBody(required = false) ApiRequest<DailyRequest> request) {
        LocalDate asOf = request == null || request.payload() == null || request.payload().asOf() == null
                ? LocalDate.now() : request.payload().asOf();
        return ApiResponse.ok(jobs.runDaily(asOf));
    }
}
