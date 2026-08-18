package com.nforceone.nforcehq.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ClientLogView(
        UUID id, UUID userId, String employeeName, String avatarInitials,
        LocalDate workDate, String clientName, BigDecimal loggedHours) {
}
