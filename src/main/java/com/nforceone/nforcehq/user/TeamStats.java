package com.nforceone.nforcehq.user;

public record TeamStats(
        int teamSize, int employeesOnTime, int lateArrivals, int wfhOnDuty, int remoteClockIns, int needsAttentionCount) {
}
