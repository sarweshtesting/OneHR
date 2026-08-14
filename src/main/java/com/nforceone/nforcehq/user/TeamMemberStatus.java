package com.nforceone.nforcehq.user;

import java.util.UUID;

public record TeamMemberStatus(
        UUID id,
        String name,
        String jobTitle,
        String avatarInitials,
        String status,
        String clockInTime) {
}
