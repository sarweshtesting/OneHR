package com.nforceone.nforcehq.user;

import java.util.UUID;

public record PunctualityEntry(
        UUID userId, String userName, String jobTitle, String avatarInitials,
        int onTimeDays, int totalDays, double onTimePercent) {
}
