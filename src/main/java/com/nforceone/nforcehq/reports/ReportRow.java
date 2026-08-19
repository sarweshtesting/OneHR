package com.nforceone.nforcehq.reports;

import java.time.Instant;
import java.time.LocalDate;

public record ReportRow(
        LocalDate date,
        String day,
        String mode,
        Instant clockInAt,
        Instant clockOutAt,
        Double hoursWorked,
        String status,
        String clientHours) {
}
