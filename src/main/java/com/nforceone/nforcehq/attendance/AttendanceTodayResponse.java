package com.nforceone.nforcehq.attendance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AttendanceTodayResponse(
        UUID recordId,
        LocalDate workDate,
        Instant clockInAt,
        Instant clockOutAt,
        String mode,
        String status,
        int totalBreakMinutes,
        Integer totalWorkedMinutes,
        boolean onBreak,
        List<BreakSegment> breaks) {

    static AttendanceTodayResponse empty() {
        return new AttendanceTodayResponse(null, null, null, null, null, null, 0, null, false, List.of());
    }
}
