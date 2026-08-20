package com.nforceone.nforcehq.user;

import java.util.UUID;

public record NegligenceEntry(
        UUID userId, String userName, String jobTitle, String avatarInitials,
        int lateCount, int missedClockoutCount, int mismatchCount, int totalIncidents) {
}
