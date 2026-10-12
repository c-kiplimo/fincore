package com.collins.platform.payments.api;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.idempotency.IdempotencyService;
import com.collins.platform.payments.application.PaymentService;
import com.collins.platform.payments.domain.PaymentIntent;
import com.collins.platform.payments.infrastructure.PaymentRepository.Dispute;
import com.collins.platform.payments.infrastructure.PaymentRepository.Refund;
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
import java.util.Map;

@RestController
public class PaymentController {

    public record CreatePaymentRequest(@NotBlank String merchantId, @Positive long amountMinor, @NotBlank String currency,
                                       @NotBlank @Pattern(regexp = "CARD|MOBILE_MONEY") String method, String instrument,
                                       String description) {
    }

    public record RefundRequest(long amountMinor, String reason) {
    }

    public record DisputeRequest(String reason) {
    }

    public record ResolveDisputeRequest(@NotBlank @Pattern(regexp = "WON|LOST") String outcome) {
    }

    public record SettlementRequest(@NotBlank String entityId, @NotBlank String currency, LocalDate date) {
    }

    private final PaymentService payments;
    private final IdempotencyService idempotency;

    public PaymentController(PaymentService payments, IdempotencyService idempotency) {
        this.payments = payments;
        this.idempotency = idempotency;
    }

    @PostMapping("/v1/payment-intents")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentIntent> create(@RequestHeader("Idempotency-Key") String key,
                                             @Valid @RequestBody ApiRequest<CreatePaymentRequest> request) {
        CreatePaymentRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("create-payment", key, p, PaymentIntent.class,
                () -> payments.create(p.merchantId(), p.amountMinor(), p.currency(), p.method(), p.instrument(), p.description())));
    }

    @GetMapping("/v1/payment-intents/{id}")
    public ApiResponse<PaymentIntent> get(@PathVariable String id) {
        return ApiResponse.ok(payments.get(id));
    }

    @GetMapping("/v1/payment-intents")
    public ApiResponse<List<PaymentIntent>> list(@RequestParam String merchantId) {
        return ApiResponse.ok(payments.byMerchant(merchantId));
    }

    @PostMapping("/v1/payment-intents/{id}/capture")
    public ApiResponse<PaymentIntent> capture(@PathVariable String id) {
        return ApiResponse.ok(payments.capture(id));
    }

    @PostMapping("/v1/payment-intents/{id}/cancel")
    public ApiResponse<PaymentIntent> cancel(@PathVariable String id) {
        return ApiResponse.ok(payments.cancel(id));
    }

    @PostMapping("/v1/payment-intents/{id}/refunds")
    public ApiResponse<PaymentIntent> refund(@PathVariable String id, @RequestHeader("Idempotency-Key") String key,
                                             @RequestBody ApiRequest<RefundRequest> request) {
        RefundRequest p = request.payload();
        return ApiResponse.ok(idempotency.execute("refund:" + id, key, p, PaymentIntent.class,
                () -> payments.refund(id, p.amountMinor(), p.reason())));
    }

    @GetMapping("/v1/payment-intents/{id}/refunds")
    public ApiResponse<List<Refund>> refunds(@PathVariable String id) {
        return ApiResponse.ok(payments.refunds(id));
    }

    @PostMapping("/v1/payment-intents/{id}/disputes")
    public ApiResponse<Dispute> openDispute(@PathVariable String id, @RequestHeader("Idempotency-Key") String key,
                                            @RequestBody ApiRequest<DisputeRequest> request) {
        return ApiResponse.ok(idempotency.execute("dispute:" + id, key, request.payload(), Dispute.class,
                () -> payments.openDispute(id, request.payload().reason())));
    }

    @PostMapping("/v1/disputes/{disputeId}/resolve")
    public ApiResponse<Dispute> resolve(@PathVariable String disputeId, @Valid @RequestBody ApiRequest<ResolveDisputeRequest> request) {
        return ApiResponse.ok(payments.resolveDispute(disputeId, request.payload().outcome()));
    }

    @PostMapping("/v1/settlements")
    public ApiResponse<PaymentService.SettlementResult> settle(@Valid @RequestBody ApiRequest<SettlementRequest> request) {
        SettlementRequest p = request.payload();
        return ApiResponse.ok(payments.settle(p.entityId(), p.currency(), p.date()));
    }

    @GetMapping("/v1/settlements")
    public ApiResponse<List<Map<String, Object>>> settlements(@RequestParam String entityId) {
        return ApiResponse.ok(payments.settlements(entityId));
    }
}
