package com.nforceone.nforcehq.attendance;

import java.time.LocalDate;
import java.util.UUID;

/** type is one of LATE_ARRIVAL, MISSED_CLOCKOUT, HOURS_MISMATCH. pseudoId is only
 * populated for HOURS_MISMATCH (the only type with a resolve action today). */
public record ExceptionView(
        String type, UUID userId, String userName, LocalDate workDate, String description, String pseudoId) {
}
