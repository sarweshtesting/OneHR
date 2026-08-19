package com.nforceone.nforcehq.user;

import java.time.LocalDate;

public record DailyOnTimeCount(LocalDate date, int onTimeCount) {
}
