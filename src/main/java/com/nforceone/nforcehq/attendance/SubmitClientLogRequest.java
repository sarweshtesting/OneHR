package com.nforceone.nforcehq.attendance;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Exactly one of clientId (an existing client) or newClientName (a client not yet in
 * the roster) must be supplied — the frontend's client picker offers both as one flow.
 */
public record SubmitClientLogRequest(
        @NotNull LocalDate workDate,
        UUID clientId,
        String newClientName,
        String newClientContact,
        String newClientNotes,
        @NotNull BigDecimal loggedHours) {
}
