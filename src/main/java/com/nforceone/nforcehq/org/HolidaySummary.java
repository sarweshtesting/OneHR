package com.nforceone.nforcehq.org;

import java.time.LocalDate;
import java.util.UUID;

public record HolidaySummary(UUID id, LocalDate date, String name) {

    static HolidaySummary from(Holiday holiday) {
        return new HolidaySummary(holiday.getId(), holiday.getHolidayDate(), holiday.getName());
    }
}
