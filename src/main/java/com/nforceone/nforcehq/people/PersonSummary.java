package com.nforceone.nforcehq.people;

import java.util.UUID;

public record PersonSummary(
        UUID id, String fullName, String email, String phone, String role,
        String jobTitle, String departmentName, String avatarInitials, boolean active) {
}
