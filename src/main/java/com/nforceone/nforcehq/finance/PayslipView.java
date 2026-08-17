package com.nforceone.nforcehq.finance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PayslipView(
        UUID id, UUID userId, String employeeName, String avatarInitials,
        LocalDate periodMonth, BigDecimal grossPay, BigDecimal deductions, BigDecimal netPay, String status) {
}
