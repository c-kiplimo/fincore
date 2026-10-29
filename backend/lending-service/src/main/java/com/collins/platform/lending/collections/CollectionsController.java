package com.collins.platform.lending.collections;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.lending.collections.CollectionsRepository.RecoveryCase;
import com.collins.platform.lending.collections.CollectionsRepository.Step;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CollectionsController {

    public record OpenCaseRequest(@NotBlank String entityId, @NotBlank @Pattern(regexp = "INDIVIDUAL|MERCHANT") String debtorType,
                                  @NotBlank String debtorId, @NotBlank String debtAccount, @NotBlank String currency,
                                  @Positive long amountMinor, String reason) {
    }

    public record WalletRef(@NotBlank String walletId) {
    }

    public record MerchantRef(@NotBlank String merchantId) {
    }

    public record SweepRequest(@NotBlank String walletId, long amountMinor) {
    }

    private final CollectionsService collections;

    public CollectionsController(CollectionsService collections) {
        this.collections = collections;
    }

    @PostMapping("/v1/recovery-cases")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecoveryCase> open(@Valid @RequestBody ApiRequest<OpenCaseRequest> request) {
        OpenCaseRequest p = request.payload();
        return ApiResponse.ok(collections.open(p.entityId(), p.debtorType(), p.debtorId(), "BALANCE", p.debtAccount(), p.currency(),
                p.amountMinor(), p.reason(), null));
    }

    @PostMapping("/v1/recovery-cases/from-negative-wallet")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecoveryCase> fromWallet(@Valid @RequestBody ApiRequest<WalletRef> request) {
        return ApiResponse.ok(collections.fromNegativeWallet(request.payload().walletId()));
    }

    @PostMapping("/v1/recovery-cases/from-negative-merchant")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RecoveryCase> fromMerchant(@Valid @RequestBody ApiRequest<MerchantRef> request) {
        return ApiResponse.ok(collections.fromNegativeMerchant(request.payload().merchantId()));
    }

    @GetMapping("/v1/recovery-cases/{caseId}")
    public ApiResponse<RecoveryCase> get(@PathVariable String caseId) {
        return ApiResponse.ok(collections.get(caseId));
    }

    @GetMapping("/v1/recovery-cases")
    public ApiResponse<List<RecoveryCase>> byDebtor(@RequestParam String debtorId) {
        return ApiResponse.ok(collections.byDebtor(debtorId));
    }

    @GetMapping("/v1/recovery-cases/{caseId}/steps")
    public ApiResponse<List<Step>> steps(@PathVariable String caseId) {
        return ApiResponse.ok(collections.steps(caseId));
    }

    @PostMapping("/v1/recovery-cases/{caseId}/run")
    public ApiResponse<RecoveryCase> run(@PathVariable String caseId) {
        return ApiResponse.ok(collections.run(caseId));
    }

    @PostMapping("/v1/recovery-cases/{caseId}/write-off")
    public ApiResponse<RecoveryCase> writeOff(@PathVariable String caseId) {
        return ApiResponse.ok(collections.writeOff(caseId));
    }

    @PostMapping("/v1/recovery-cases/{caseId}/sweep")
    public ApiResponse<RecoveryCase> sweep(@PathVariable String caseId, @Valid @RequestBody ApiRequest<SweepRequest> request) {
        return ApiResponse.ok(collections.sweepAfterWriteOff(caseId, request.payload().walletId(), request.payload().amountMinor()));
    }
}
