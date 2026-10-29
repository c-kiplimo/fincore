package com.collins.platform.accounts.api;

import com.collins.platform.accounts.application.MerchantService;
import com.collins.platform.accounts.application.WalletService;
import com.collins.platform.accounts.application.WalletService.WalletView;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Individual;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Merchant;
import com.collins.platform.accounts.infrastructure.AccountsRepository.ReserveHold;
import com.collins.platform.accounts.infrastructure.AccountsRepository.Transfer;
import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.idempotency.IdempotencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.Map;

@RestController
public class AccountsController {

    public record OnboardMerchantRequest(@NotBlank String entityId, @NotBlank String name, @NotBlank @Size(min = 2, max = 2) String country,
                                         @NotBlank String settlementCurrency, @Min(0) @Max(10000) int reservePercentBp,
                                         @Min(0) int reserveDays) {
    }

    public record RegisterIndividualRequest(@NotBlank String entityId, @NotBlank String name,
                                            @NotBlank @Size(min = 2, max = 2) String country, @NotBlank String mainCurrency) {
    }

    public record OpenWalletRequest(@NotBlank String currency, @NotBlank @Pattern(regexp = "MAIN|SAVINGS") String kind) {
    }

    public record TopUpRequest(@Positive long amountMinor, @NotBlank String method) {
    }

    public record TransferRequest(@NotBlank String fromWalletId, @NotBlank String toWalletId, @Positive long amountMinor) {
    }

    public record WalletPaymentRequest(@NotBlank String walletId, @NotBlank String merchantId, @Positive long amountMinor) {
    }

    public record DisbursementRequest(@NotBlank String merchantId, @NotBlank String walletId, @Positive long amountMinor) {
    }

    private final MerchantService merchants;
    private final WalletService wallets;
    private final IdempotencyService idempotency;

    public AccountsController(MerchantService merchants, WalletService wallets, IdempotencyService idempotency) {
        this.merchants = merchants;
        this.wallets = wallets;
        this.idempotency = idempotency;
    }

    // ---------- merchants ----------

    @PostMapping("/v1/merchants")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Merchant> onboard(@Valid @RequestBody ApiRequest<OnboardMerchantRequest> request) {
        OnboardMerchantRequest p = request.payload();
        return ApiResponse.ok(merchants.onboard(p.entityId(), p.name(), p.country(), p.settlementCurrency(), p.reservePercentBp(), p.reserveDays()));
    }

    @GetMapping("/v1/merchants/{merchantId}")
    public ApiResponse<Merchant> merchant(@PathVariable String merchantId) {
        return ApiResponse.ok(merchants.get(merchantId));
    }

    @GetMapping("/v1/merchants")
    public ApiResponse<List<Merchant>> merchants(@RequestParam String entityId) {
        return ApiResponse.ok(merchants.list(entityId));
    }

    @GetMapping("/v1/merchants/{merchantId}/balances")
    public ApiResponse<MerchantService.MerchantBalances> balances(@PathVariable String merchantId) {
        return ApiResponse.ok(merchants.balances(merchantId));
    }

    @GetMapping("/v1/merchants/{merchantId}/reserves")
    public ApiResponse<List<ReserveHold>> reserves(@PathVariable String merchantId) {
        return ApiResponse.ok(merchants.holds(merchantId));
    }

    @PostMapping("/v1/reserves/release-due")
    public ApiResponse<Map<String, Integer>> releaseDue(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.ok(Map.of("released", merchants.releaseDue(asOf == null ? LocalDate.now() : asOf)));
    }

    // ---------- individuals and wallets ----------

    @PostMapping("/v1/individuals")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Individual> register(@Valid @RequestBody ApiRequest<RegisterIndividualRequest> request) {
        RegisterIndividualRequest p = request.payload();
        return ApiResponse.ok(wallets.register(p.entityId(), p.name(), p.country(), p.mainCurrency()));
    }

    @GetMapping("/v1/individuals/{individualId}")
    public ApiResponse<Individual> individual(@PathVariable String individualId) {
        return ApiResponse.ok(wallets.individual(individualId));
    }

    @PostMapping("/v1/individuals/{individualId}/wallets")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WalletView> openWallet(@PathVariable String individualId, @Valid @RequestBody ApiRequest<OpenWalletRequest> request) {
        return ApiResponse.ok(wallets.openWallet(individualId, request.payload().currency(), request.payload().kind()));
    }

    @GetMapping("/v1/individuals/{individualId}/wallets")
    public ApiResponse<List<WalletView>> walletsOf(@PathVariable String individualId) {
        return ApiResponse.ok(wallets.wallets(individualId));
    }

    @GetMapping("/v1/wallets/{walletId}")
    public ApiResponse<WalletView> wallet(@PathVariable String walletId) {
        return ApiResponse.ok(wallets.wallet(walletId));
    }

    @GetMapping("/v1/wallets/{walletId}/transfers")
    public ApiResponse<List<Transfer>> history(@PathVariable String walletId) {
        return ApiResponse.ok(wallets.history(walletId));
    }

    @PostMapping("/v1/wallets/{walletId}/top-ups")
    public ApiResponse<Transfer> topUp(@PathVariable String walletId, @RequestHeader("Idempotency-Key") String key,
                                       @Valid @RequestBody ApiRequest<TopUpRequest> request) {
        TopUpRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("topup:" + walletId, key, p, Transfer.class,
                () -> wallets.topUp(walletId, p.amountMinor(), p.method())));
    }

    @PostMapping("/v1/transfers")
    public ApiResponse<Transfer> transfer(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ApiRequest<TransferRequest> request) {
        TransferRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("transfer", key, p, Transfer.class,
                () -> wallets.transfer(p.fromWalletId(), p.toWalletId(), p.amountMinor())));
    }

    @PostMapping("/v1/conversions")
    public ApiResponse<Transfer> convert(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody ApiRequest<TransferRequest> request) {
        TransferRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("conversion", key, p, Transfer.class,
                () -> wallets.convert(p.fromWalletId(), p.toWalletId(), p.amountMinor())));
    }

    @PostMapping("/v1/wallet-payments")
    public ApiResponse<Transfer> payMerchant(@RequestHeader("Idempotency-Key") String key,
                                             @Valid @RequestBody ApiRequest<WalletPaymentRequest> request) {
        WalletPaymentRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("wallet-payment", key, p, Transfer.class,
                () -> wallets.payMerchant(p.walletId(), p.merchantId(), p.amountMinor())));
    }

    @PostMapping("/v1/merchant-disbursements")
    public ApiResponse<Transfer> disburse(@RequestHeader("Idempotency-Key") String key,
                                          @Valid @RequestBody ApiRequest<DisbursementRequest> request) {
        DisbursementRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("disbursement", key, p, Transfer.class,
                () -> wallets.disburseFromMerchant(p.merchantId(), p.walletId(), p.amountMinor())));
    }
}
