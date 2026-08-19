package com.nforceone.nforcehq.attendance;

import java.util.UUID;

public record ClockInRequest(AttendanceMode mode, UUID clientId) {
}
