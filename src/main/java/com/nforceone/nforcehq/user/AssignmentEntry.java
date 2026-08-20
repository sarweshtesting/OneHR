package com.nforceone.nforcehq.user;

import java.util.UUID;

public record AssignmentEntry(
        UUID userId, String userName, String avatarInitials, String jobTitle, String departmentName, String email) {
}
