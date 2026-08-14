package com.nforceone.nforcehq.security;

import java.util.UUID;

/**
 * The authenticated principal attached to SecurityContext for every JWT-authenticated
 * request. homeOrganizationId is the org fixed in the token (null for PLATFORM_ADMIN);
 * effectiveOrganizationId is what tenant context was actually applied for this request
 * (home org for normal users, or the validated X-Organization-Id header for a
 * platform admin — null if a platform admin hasn't selected one yet).
 */
public record JwtPrincipal(
        UUID userId,
        UUID homeOrganizationId,
        UUID effectiveOrganizationId,
        String role,
        String name,
        String email) {
}
