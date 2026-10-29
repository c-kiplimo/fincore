package com.collins.platform.payments.api;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.payments.api.dto.SettlementRequest;
import com.collins.platform.payments.application.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/settlements")
public class SettlementController {

    private final PaymentService paymentService;

    public SettlementController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<PaymentService.SettlementResult> createSettlement(
            @Valid @RequestBody ApiRequest<SettlementRequest> request) {

        PaymentService.SettlementResult result = paymentService.settle(request.payload());
        return ApiResponse.ok(result);
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listSettlements(@RequestParam String entityId) {
        return ApiResponse.ok(paymentService.settlements(entityId));
    }
}