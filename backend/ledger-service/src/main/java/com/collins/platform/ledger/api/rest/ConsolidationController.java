package com.collins.platform.ledger.api.rest;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.ledger.api.rest.LedgerDtos.ConsolidationRequest;
import com.collins.platform.ledger.application.ConsolidationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsolidationController {

    private final ConsolidationService consolidation;

    public ConsolidationController(ConsolidationService consolidation) {
        this.consolidation = consolidation;
    }

    @PostMapping("/v1/consolidation")
    public ApiResponse<ConsolidationService.ConsolidatedReport> consolidate(@Valid @RequestBody ApiRequest<ConsolidationRequest> request) {
        ConsolidationRequest p = request.payload();
        return ApiResponse.ok(consolidation.consolidate(p.entityIds(), p.reportingCurrency(), p.asOf(), p.closingRates(), p.averageRates()));
    }
}
