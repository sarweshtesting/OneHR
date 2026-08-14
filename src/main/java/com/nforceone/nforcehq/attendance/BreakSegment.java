package com.nforceone.nforcehq.attendance;

import java.time.Instant;

public record BreakSegment(Instant startAt, Instant endAt, String type) {

    static BreakSegment from(AttendanceBreak attendanceBreak) {
        return new BreakSegment(
                attendanceBreak.getBreakStartAt(),
                attendanceBreak.getBreakEndAt(),
                attendanceBreak.getBreakType().name());
    }
}
