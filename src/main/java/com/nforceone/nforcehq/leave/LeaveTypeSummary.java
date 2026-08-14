package com.nforceone.nforcehq.leave;

import java.math.BigDecimal;
import java.util.UUID;

public record LeaveTypeSummary(UUID id, String code, String name, String colorToken, BigDecimal annualQuotaDays) {

    static LeaveTypeSummary from(LeaveType leaveType) {
        return new LeaveTypeSummary(leaveType.getId(), leaveType.getCode().name(), leaveType.getName(),
                leaveType.getColorToken(), leaveType.getAnnualQuotaDays());
    }
}
