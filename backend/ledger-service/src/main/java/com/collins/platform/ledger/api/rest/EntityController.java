package com.collins.platform.ledger.api.rest;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.ledger.api.rest.LedgerDtos.CreateEntityRequest;
import com.collins.platform.ledger.api.rest.LedgerDtos.RevaluationRequest;
import com.collins.platform.ledger.application.BalanceService;
import com.collins.platform.ledger.application.BalanceView;
import com.collins.platform.ledger.application.EntityService;
import com.collins.platform.ledger.application.InvariantService;
import com.collins.platform.ledger.application.PeriodService;
import com.collins.platform.ledger.application.ReportService;
import com.collins.platform.ledger.application.RevaluationService;
import com.collins.platform.ledger.infrastructure.LedgerRows.ChartAccount;
import com.collins.platform.ledger.infrastructure.LedgerRows.EntityRow;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** Legal entities, their chart, balances, statements, reports and period close. */
@RestController
@RequestMapping("/v1/entities")
public class EntityController {

    private final EntityService entities;
    private final BalanceService balances;
    private final ReportService reports;
    private final PeriodService periods;
    private final RevaluationService revaluation;
    private final InvariantService invariants;

    public EntityController(EntityService entities, BalanceService balances, ReportService reports,
                            PeriodService periods, RevaluationService revaluation, InvariantService invariants) {
        this.entities = entities;
        this.balances = balances;
        this.reports = reports;
        this.periods = periods;
        this.revaluation = revaluation;
        this.invariants = invariants;
    }

    @GetMapping
    public ApiResponse<List<EntityRow>> list() {
        return ApiResponse.ok(entities.list());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EntityRow> create(@Valid @RequestBody ApiRequest<CreateEntityRequest> request) {
        CreateEntityRequest p = request.payload();
        return ApiResponse.ok(entities.create(new EntityRow(p.entityId(), p.name(), p.country(), p.functionalCurrency(), p.timezone()),
                p.copyChartFrom()));
    }

    @GetMapping("/{entityId}/chart")
    public ApiResponse<List<ChartAccount>> chart(@PathVariable String entityId) {
        return ApiResponse.ok(entities.chart(entityId));
    }

    @GetMapping("/{entityId}/accounts/{account}/balance")
    public ApiResponse<BalanceView> balance(@PathVariable String entityId, @PathVariable String account,
                                            @RequestParam String currency,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.ok(asOf == null ? balances.current(entityId, account, currency) : balances.asOf(entityId, account, currency, asOf));
    }

    @GetMapping("/{entityId}/accounts/{account}/statement")
    public ApiResponse<BalanceService.Statement> statement(@PathVariable String entityId, @PathVariable String account,
                                                           @RequestParam String currency,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(balances.statement(entityId, account, currency, from, to));
    }

    @GetMapping("/{entityId}/reports/trial-balance")
    public ApiResponse<ReportService.TrialBalance> trialBalance(@PathVariable String entityId,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.ok(reports.trialBalance(entityId, asOf == null ? LocalDate.now() : asOf));
    }

    @GetMapping("/{entityId}/reports/balance-sheet")
    public ApiResponse<ReportService.BalanceSheet> balanceSheet(@PathVariable String entityId, @RequestParam String currency,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.ok(reports.balanceSheet(entityId, asOf == null ? LocalDate.now() : asOf, currency));
    }

    @GetMapping("/{entityId}/reports/income-statement")
    public ApiResponse<ReportService.IncomeStatement> incomeStatement(@PathVariable String entityId, @RequestParam String currency,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reports.incomeStatement(entityId, from, to, currency));
    }

    @GetMapping("/{entityId}/invariants")
    public ApiResponse<InvariantService.InvariantReport> invariants(@PathVariable String entityId) {
        return ApiResponse.ok(invariants.check(entityId));
    }

    @GetMapping("/{entityId}/periods")
    public ApiResponse<List<Map<String, Object>>> periods(@PathVariable String entityId) {
        return ApiResponse.ok(periods.periods(entityId));
    }

    @PostMapping("/{entityId}/periods/{period}/close")
    public ApiResponse<PeriodService.CloseResult> close(@PathVariable String entityId, @PathVariable String period) {
        return ApiResponse.ok(periods.close(entityId, YearMonth.parse(period)));
    }

    @PostMapping("/{entityId}/revaluations")
    public ApiResponse<RevaluationService.RevaluationResult> revalue(@PathVariable String entityId,
                                                                     @Valid @RequestBody ApiRequest<RevaluationRequest> request) {
        return ApiResponse.ok(revaluation.revalue(entityId, request.payload().asOf(), request.payload().rates()));
    }
}
