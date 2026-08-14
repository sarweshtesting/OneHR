package com.nforceone.nforcehq.dashboard;

public record DashboardStats(
        int presentToday,
        int totalHeadcount,
        int pendingLeaveCount,
        int pendingRegularizationCount,
        int mismatchesFlagged,
        double avgHoursThisWeek) {
}
