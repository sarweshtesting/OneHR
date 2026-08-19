package com.nforceone.nforcehq.user;

import java.util.List;
import java.util.UUID;

public record TeamCalendarRow(UUID userId, String userName, String avatarInitials, List<TeamCalendarDay> days) {
}
