package com.nforceone.nforcehq.user;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String name,
        String email,
        String role,
        String jobTitle,
        UUID orgId,
        String orgName,
        String avatarPhotoDataUri) {
}
