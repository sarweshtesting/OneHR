package com.nforceone.nforcehq.attendance;

import java.time.LocalDate;
import java.util.UUID;

/**
 * pseudoId encodes userId + workDate ("<userId>::<workDate>") since
 * v_attendance_mismatches is a computed join with no natural row identity —
 * used to address a specific mismatch for the resolve endpoint.
 */
public record MismatchView(
        String pseudoId,
        UUID userId,
        String userName,
        LocalDate workDate,
        double internalHours,
        String clientName,
        double clientHours,
        double hoursDelta) {
}
