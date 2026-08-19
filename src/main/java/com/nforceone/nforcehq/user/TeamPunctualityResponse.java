package com.nforceone.nforcehq.user;

import java.util.List;

public record TeamPunctualityResponse(
        double avgOnTimePerDay, int minOnTimePerDay, int maxOnTimePerDay,
        List<PunctualityEntry> leaderboard, List<DailyOnTimeCount> dailyCounts) {
}
