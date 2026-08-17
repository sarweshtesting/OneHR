package com.nforceone.nforcehq.leave;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * requestType is "LEAVE" for now; a "REGULARIZATION" variant joins this same shape
 * once attendance_regularization_requests gets a service (a later phase), matching
 * the mock's "My leave history" table which mixes both request kinds in one list.
 */
public record RequestHistoryItem(
        UUID id,
        String requestType,
        String typeLabel,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal days,
        String status,
        Instant createdAt) {
}
