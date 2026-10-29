package com.collins.platform.payments.api;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.idempotency.IdempotencyService;
import com.collins.platform.payments.api.dto.CreatePaymentRequest;
import com.collins.platform.payments.api.dto.RefundRequest;
import com.collins.platform.payments.application.PaymentService;
import com.collins.platform.payments.domain.PaymentIntent;
import com.collins.platform.payments.infrastructure.PaymentRepository.Refund;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.collins.platform.payments.api.IdempotencyScopes.CREATE_PAYMENT;
import static com.collins.platform.payments.api.IdempotencyScopes.refundScope;

@RestController
@RequestMapping("/v1/payment-intents")
public class PaymentIntentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public PaymentIntentController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentIntent> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ApiRequest<CreatePaymentRequest> request) {

        PaymentIntent result = idempotencyService.execute(
            CREATE_PAYMENT,
            idempotencyKey,
            request.payload(),
            PaymentIntent.class,
            () -> paymentService.create(request.payload())
        );
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<PaymentIntent> get(@PathVariable String id) {
        return ApiResponse.ok(paymentService.get(id));
    }

    @GetMapping
    public ApiResponse<List<PaymentIntent>> list(@RequestParam String merchantId) {
        return ApiResponse.ok(paymentService.byMerchant(merchantId));
    }

    @PostMapping("/{id}/capture")
    public ApiResponse<PaymentIntent> capture(@PathVariable String id) {
        return ApiResponse.ok(paymentService.capture(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<PaymentIntent> cancel(@PathVariable String id) {
        return ApiResponse.ok(paymentService.cancel(id));
    }

    @PostMapping("/{id}/refunds")
    public ApiResponse<PaymentIntent> refund(
            @PathVariable String id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ApiRequest<RefundRequest> request) {

        PaymentIntent result = idempotencyService.execute(
            refundScope(id),
            idempotencyKey,
            request.payload(),
            PaymentIntent.class,
            () -> paymentService.refund(id, request.payload())
        );
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}/refunds")
    public ApiResponse<List<Refund>> getRefunds(@PathVariable String id) {
        return ApiResponse.ok(paymentService.refunds(id));
    }
}