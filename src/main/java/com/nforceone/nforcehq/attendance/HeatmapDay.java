package com.nforceone.nforcehq.attendance;

import java.time.LocalDate;

/** type is one of: full, wfh, partial, weekend, none (no record). */
public record HeatmapDay(LocalDate date, String type) {
}
