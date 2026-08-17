package com.nforceone.nforcehq.user;

/**
 * Ordered roughly low-to-high privilege within an organization. PLATFORM_ADMIN sits
 * outside this org-scoped hierarchy entirely — it's nforceone.com's own cross-tenant
 * staff role, always granted org-wide-equivalent access wherever these roles are checked.
 */
public enum Role {
    EMPLOYEE,
    MANAGER,
    HR_ADMIN,
    ADMIN,
    SUPER_ADMIN,
    PLATFORM_ADMIN
}
