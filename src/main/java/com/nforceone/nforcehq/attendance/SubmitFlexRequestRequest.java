package com.nforceone.nforcehq.attendance;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitFlexRequestRequest(
        @NotNull FlexRequestType type, @NotNull LocalDate workDate, BigDecimal hours, String reason) {
}
