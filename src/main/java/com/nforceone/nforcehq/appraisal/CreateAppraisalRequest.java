package com.nforceone.nforcehq.appraisal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateAppraisalRequest(
        @NotNull UUID userId,
        @NotBlank String cycleName,
        String overallRating,
        String strengths,
        String areasForImprovement,
        String goalsNextCycle) {
}
