package com.nforceone.nforcehq.common;

import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.Role;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.List;
import java.util.UUID;

/**
 * Shared "whose requests can this caller act on" resolution for the approvals
 * inbox, mismatches, and dashboard stats: a MANAGER sees their direct reports;
 * ADMIN/PLATFORM_ADMIN see the whole organization. EMPLOYEE never reaches these
 * endpoints (blocked by @PreAuthorize), but resolves to an empty scope defensively.
 */
public final class ApprovalScope {

    private ApprovalScope() {
    }

    public static List<UUID> resolve(JwtPrincipal principal, UserRepository userRepository, UUID organizationId) {
        if (Role.MANAGER.name().equals(principal.role())) {
            return userRepository.findByManagerIdAndOrganizationId(principal.userId(), organizationId).stream()
                    .map(User::getId)
                    .toList();
        }
        if (Role.ADMIN.name().equals(principal.role()) || Role.PLATFORM_ADMIN.name().equals(principal.role())) {
            return userRepository.findByOrganizationId(organizationId).stream()
                    .map(User::getId)
                    .toList();
        }
        return List.of();
    }
}
