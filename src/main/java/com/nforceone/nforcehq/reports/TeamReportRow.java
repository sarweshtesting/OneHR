package com.nforceone.nforcehq.reports;

import java.time.Instant;
import java.time.LocalDate;

public record TeamReportRow(
        LocalDate date,
        String employeeName,
        String day,
        String mode,
        Instant clockInAt,
        Instant clockOutAt,
        Double hoursWorked,
        String status) {
}
