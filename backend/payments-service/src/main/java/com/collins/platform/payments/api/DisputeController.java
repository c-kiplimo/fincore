package com.collins.platform.payments.api;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.idempotency.IdempotencyService;
import com.collins.platform.payments.api.dto.DisputeRequest;
import com.collins.platform.payments.api.dto.ResolveDisputeRequest;
import com.collins.platform.payments.application.PaymentService;
import com.collins.platform.payments.infrastructure.PaymentRepository.Dispute;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static com.collins.platform.payments.api.IdempotencyScopes.disputeScope;

@RestController
public class DisputeController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public DisputeController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/v1/payment-intents/{id}/disputes")
    public ApiResponse<Dispute> openDispute(
            @PathVariable String id,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ApiRequest<DisputeRequest> request) {

        Dispute result = idempotencyService.execute(
            disputeScope(id),
            idempotencyKey,
            request.payload(),
            Dispute.class,
            () -> paymentService.openDispute(id, request.payload())
        );
        return ApiResponse.ok(result);
    }

    @PostMapping("/v1/disputes/{disputeId}/resolve")
    public ApiResponse<Dispute> resolveDispute(
            @PathVariable String disputeId,
            @Valid @RequestBody ApiRequest<ResolveDisputeRequest> request) {

        Dispute result = paymentService.resolveDispute(disputeId, request.payload().outcome());
        return ApiResponse.ok(result);
    }
}