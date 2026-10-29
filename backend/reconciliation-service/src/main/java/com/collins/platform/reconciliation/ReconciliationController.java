package com.collins.platform.reconciliation;

import com.collins.platform.commons.api.ApiRequest;
import com.collins.platform.commons.api.ApiResponse;
import com.collins.platform.commons.exception.BusinessRuleException;
import com.collins.platform.reconciliation.ReconModels.Explanation;
import com.collins.platform.reconciliation.ReconModels.Line;
import com.collins.platform.reconciliation.ReconModels.LineInput;
import com.collins.platform.reconciliation.ReconModels.RunResult;
import com.collins.platform.reconciliation.ReconModels.Safeguarding;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ReconciliationController {

    public record RunRequest(@NotBlank String entityId, @NotBlank @Pattern(regexp = "BANK|NETWORK") String source,
                             @NotBlank String currency, @NotNull LocalDate statementDate, @NotNull List<LineInput> lines) {
    }

    public record BankImportRequest(@NotBlank String entityId, @NotBlank String currency, @NotNull LocalDate date) {
    }

    public record ResolveRequest(@NotBlank String counterAccount) {
    }

    private final ReconciliationService recon;

    public ReconciliationController(ReconciliationService recon) {
        this.recon = recon;
    }

    @PostMapping("/v1/reconciliation-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RunResult> run(@Valid @RequestBody ApiRequest<RunRequest> request) {
        RunRequest p = request.payload();
        return ApiResponse.ok(recon.reconcile(p.entityId(), p.source(), p.currency(), p.statementDate(), p.lines()));
    }

    /** Pulls today's statement from the (simulated) bank and reconciles it. */
    @PostMapping("/v1/reconciliation-runs/from-bank")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RunResult> fromBank(@Valid @RequestBody ApiRequest<BankImportRequest> request) {
        BankImportRequest p = request.payload();
        return ApiResponse.ok(recon.reconcileFromBank(p.entityId(), p.currency(), p.date()));
    }

    /** CSV upload with header: externalRef,amountMinor,valueDate,description */
    @PostMapping(value = "/v1/reconciliation-runs/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RunResult> csv(@RequestParam String entityId, @RequestParam String currency,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate statementDate,
                                      @RequestPart("file") MultipartFile file) throws IOException {
        List<LineInput> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String row;
            boolean header = true;
            while ((row = reader.readLine()) != null) {
                if (header || row.isBlank()) {
                    header = false;
                    continue;
                }
                String[] cols = row.split(",", 4);
                if (cols.length < 2) {
                    throw new BusinessRuleException("BAD_CSV", "Each row needs at least externalRef,amountMinor");
                }
                lines.add(new LineInput(cols[0].trim(), Long.parseLong(cols[1].trim()),
                        cols.length > 2 && !cols[2].isBlank() ? LocalDate.parse(cols[2].trim()) : statementDate,
                        cols.length > 3 ? cols[3].trim() : null));
            }
        }
        return ApiResponse.ok(recon.reconcile(entityId, "BANK", currency, statementDate, lines));
    }

    @GetMapping("/v1/reconciliation-runs/{runId}")
    public ApiResponse<RunResult> get(@PathVariable String runId, @RequestParam(required = false) String status) {
        return ApiResponse.ok(recon.get(runId, status));
    }

    @PostMapping("/v1/statement-lines/{lineId}/explain")
    public ApiResponse<Explanation> explain(@PathVariable String lineId) {
        return ApiResponse.ok(recon.explain(lineId));
    }

    @PostMapping("/v1/statement-lines/{lineId}/suspense")
    public ApiResponse<Line> suspense(@PathVariable String lineId) {
        return ApiResponse.ok(recon.toSuspense(lineId));
    }

    @PostMapping("/v1/statement-lines/{lineId}/resolve")
    public ApiResponse<Line> resolve(@PathVariable String lineId, @Valid @RequestBody ApiRequest<ResolveRequest> request) {
        return ApiResponse.ok(recon.resolveSuspense(lineId, request.payload().counterAccount()));
    }

    @GetMapping("/v1/safeguarding")
    public ApiResponse<Safeguarding> safeguarding(@RequestParam String entityId, @RequestParam String currency) {
        return ApiResponse.ok(recon.safeguarding(entityId, currency));
    }
}
