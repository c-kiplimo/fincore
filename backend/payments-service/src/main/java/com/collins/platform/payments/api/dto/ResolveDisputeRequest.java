package com.collins.platform.payments.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResolveDisputeRequest(
    @NotBlank @Pattern(regexp = "WON|LOST") String outcome
) {}