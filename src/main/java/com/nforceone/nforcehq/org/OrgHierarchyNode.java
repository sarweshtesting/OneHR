package com.nforceone.nforcehq.org;

import java.util.UUID;

/** Flat list; the frontend assembles the tree by matching id → managerId. */
public record OrgHierarchyNode(
        UUID id, String fullName, String jobTitle, String role, String avatarInitials, UUID managerId) {
}
