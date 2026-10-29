package com.collins.platform.pricing.api;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.pricing.api.PricingDtos.ConsumeQuoteRequest;
import com.collins.platform.pricing.api.PricingDtos.CreateQuoteRequest;
import com.collins.platform.pricing.application.QuoteService;
import com.collins.platform.pricing.domain.QuoteModel;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/quotes")
public class QuoteController {

    private final QuoteService quotes;

    public QuoteController(QuoteService quotes) {
        this.quotes = quotes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<QuoteModel> create(@Valid @RequestBody ApiRequest<CreateQuoteRequest> request) {
        CreateQuoteRequest p = request.payload();
        return ApiResponse.ok(quotes.create(new QuoteService.QuoteRequest(p.entityId(), p.product(), p.amountMinor(), p.currency(),
                p.targetCurrency(), p.customerCountry(), p.customerType(), p.transactionDate())));
    }

    @GetMapping("/{quoteId}")
    public ApiResponse<QuoteModel> get(@PathVariable String quoteId) {
        return ApiResponse.ok(quotes.get(quoteId));
    }

    @PostMapping("/{quoteId}/consume")
    public ApiResponse<QuoteModel> consume(@PathVariable String quoteId, @Valid @RequestBody ApiRequest<ConsumeQuoteRequest> request) {
        return ApiResponse.ok(quotes.consume(quoteId, request.payload().reference()));
    }
}
