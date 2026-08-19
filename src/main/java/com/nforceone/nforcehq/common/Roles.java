package com.nforceone.nforcehq.common;

import java.util.Set;

/** Shared role-group constants so every RBAC check across modules stays in sync. */
public final class Roles {

    /** Direct-report or whole-org approval/visibility scope — everyone above plain EMPLOYEE. */
    public static final Set<String> MANAGER_UP = Set.of("MANAGER", "HR_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN");

    /** Whole-org visibility regardless of direct-report chain (HR/admin tiers + cross-org platform staff). */
    public static final Set<String> ORG_WIDE = Set.of("HR_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN");

    /** Finance module (including chat) — leadership and HR, not plain employees. */
    public static final Set<String> FINANCE = Set.of("MANAGER", "HR_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN");

    /** Can create/invite/manage people and org structure — HR and the org's top admin tier. */
    public static final Set<String> CAN_MANAGE_PEOPLE = Set.of("HR_ADMIN", "SUPER_ADMIN", "PLATFORM_ADMIN");

    private Roles() {
    }
}
