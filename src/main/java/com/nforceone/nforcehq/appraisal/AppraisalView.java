package com.nforceone.nforcehq.appraisal;

import java.time.Instant;
import java.util.UUID;

public record AppraisalView(
        UUID id,
        UUID userId,
        String userName,
        String reviewerName,
        String cycleName,
        String overallRating,
        String strengths,
        String areasForImprovement,
        String goalsNextCycle,
        String status,
        Instant createdAt) {
}
