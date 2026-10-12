package com.collins.platform.payouts;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.idempotency.IdempotencyService;
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

import java.util.List;

@RestController
public class PayoutController {

    public record CreatePayoutRequest(@NotBlank @Pattern(regexp = "MERCHANT|WALLET") String beneficiaryType,
                                      @NotBlank String beneficiaryId, @Positive long amountMinor,
                                      @NotBlank @Pattern(regexp = "BANK|MOBILE_MONEY") String destinationType,
                                      @NotBlank String destinationAccount) {
    }

    private final PayoutSaga saga;
    private final IdempotencyService idempotency;

    public PayoutController(PayoutSaga saga, IdempotencyService idempotency) {
        this.saga = saga;
        this.idempotency = idempotency;
    }

    @PostMapping("/v1/payouts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Payout> create(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ApiRequest<CreatePayoutRequest> request) {
        CreatePayoutRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("payout", key, p, Payout.class,
                () -> saga.start(p.beneficiaryType(), p.beneficiaryId(), p.amountMinor(), p.destinationType(), p.destinationAccount())));
    }

    @GetMapping("/v1/payouts/{payoutId}")
    public ApiResponse<Payout> get(@PathVariable String payoutId) {
        return ApiResponse.ok(saga.get(payoutId));
    }

    @GetMapping("/v1/payouts")
    public ApiResponse<List<Payout>> list(@RequestParam String beneficiaryId) {
        return ApiResponse.ok(saga.byBeneficiary(beneficiaryId));
    }

    @PostMapping("/v1/payouts/{payoutId}/retry")
    public ApiResponse<Payout> retry(@PathVariable String payoutId) {
        return ApiResponse.ok(saga.retry(payoutId));
    }
}
