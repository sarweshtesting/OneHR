package com.nforceone.nforcehq.leave;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaveBalanceView(
        UUID leaveTypeId,
        String leaveTypeName,
        String colorToken,
        BigDecimal allocatedDays,
        BigDecimal usedDays,
        BigDecimal remainingDays) {
}
