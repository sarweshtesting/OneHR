package com.nforceone.nforcehq.org;

import java.util.UUID;

/** Flat list; the frontend assembles the reporting-line tree by matching id → managerId,
 * or groups by departmentId/departmentName for the department-structure view. */
public record OrgHierarchyNode(
        UUID id, String fullName, String jobTitle, String role, String avatarInitials, UUID managerId,
        UUID departmentId, String departmentName) {
}
