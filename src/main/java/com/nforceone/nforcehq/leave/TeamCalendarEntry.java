package com.nforceone.nforcehq.leave;

import java.time.LocalDate;
import java.util.UUID;

public record TeamCalendarEntry(
        UUID userId,
        String userName,
        String avatarInitials,
        LocalDate startDate,
        LocalDate endDate) {
}
