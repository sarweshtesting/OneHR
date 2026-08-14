package com.nforceone.nforcehq.attendance;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

public record SubmitRegularizationRequest(
        @NotNull LocalDate workDate,
        Instant requestedClockIn,
        Instant requestedClockOut,
        String reason) {
}
