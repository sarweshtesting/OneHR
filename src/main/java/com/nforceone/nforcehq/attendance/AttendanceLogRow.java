package com.nforceone.nforcehq.attendance;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceLogRow(
        LocalDate workDate,
        String employeeName,
        Instant clockInAt,
        Instant clockOutAt,
        int totalBreakMinutes,
        Integer totalWorkedMinutes,
        String mode,
        String status) {
}
