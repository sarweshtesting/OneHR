package com.nforceone.nforcehq.attendance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RegularizationView(
        UUID id,
        String userName,
        LocalDate workDate,
        Instant requestedClockIn,
        Instant requestedClockOut,
        String reason,
        String status) {

    static RegularizationView from(AttendanceRegularizationRequest r, String userName) {
        return new RegularizationView(r.getId(), userName, r.getWorkDate(), r.getRequestedClockIn(),
                r.getRequestedClockOut(), r.getReason(), r.getStatus().name());
    }
}
