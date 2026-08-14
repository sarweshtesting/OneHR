package com.nforceone.nforcehq.attendance;

public record AttendanceSummaryResponse(
        double officePct,
        double wfhPct,
        double partialPct,
        double leavePct,
        double onTimeRatePct,
        double overtimeHours,
        int overtimeSessions) {
}
