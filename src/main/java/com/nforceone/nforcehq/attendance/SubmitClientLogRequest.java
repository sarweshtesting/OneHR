package com.nforceone.nforcehq.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitClientLogRequest(
        @NotNull LocalDate workDate,
        @NotBlank String clientName,
        @NotNull BigDecimal loggedHours) {
}
