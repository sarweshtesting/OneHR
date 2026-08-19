package com.nforceone.nforcehq.servicerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitServiceRequestRequest(
        @NotNull ServiceRequestType type,
        @NotBlank String subject,
        @NotBlank String description) {
}
