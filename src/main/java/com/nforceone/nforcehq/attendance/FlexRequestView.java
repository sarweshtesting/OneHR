package com.nforceone.nforcehq.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FlexRequestView(
        UUID id, UUID userId, String userName, FlexRequestType type, LocalDate workDate, BigDecimal hours,
        String reason, String status, String approverName) {

    public static FlexRequestView from(AttendanceFlexRequest r, String userName, String approverName) {
        return new FlexRequestView(r.getId(), r.getUserId(), userName, r.getType(), r.getWorkDate(), r.getHours(),
                r.getReason(), r.getStatus().name(), approverName);
    }
}
