package com.nforceone.nforcehq.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestView(
        UUID id,
        String userName,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal daysRequested,
        String status) {
}
