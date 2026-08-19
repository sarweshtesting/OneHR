package com.nforceone.nforcehq.user;

import java.time.LocalDate;

/** status is one of HOLIDAY, WEEKLY_OFF, ON_LEAVE, WFH_ON_DUTY, MISSING_ATTENDANCE, PRESENT. */
public record TeamCalendarDay(LocalDate date, String status) {
}
