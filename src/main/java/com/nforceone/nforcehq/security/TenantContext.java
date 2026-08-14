package com.nforceone.nforcehq.security;

import java.util.UUID;

/**
 * Per-request holder for the resolved tenant (organization) and calling user, read by
 * {@link com.nforceone.nforcehq.config.TenantAwareDataSource} on every connection
 * checkout so it can set the Postgres session GUCs that back Row-Level Security.
 * Must be cleared at the end of every request — see JwtAuthenticationFilter's
 * finally block — since servlet containers reuse worker threads across requests.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> USER_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId, UUID userId) {
        TENANT_ID.set(tenantId);
        USER_ID.set(userId);
    }

    public static UUID getTenantId() {
        return TENANT_ID.get();
    }

    public static String getTenantIdOrEmpty() {
        UUID tenantId = TENANT_ID.get();
        return tenantId == null ? "" : tenantId.toString();
    }

    public static String getUserIdOrEmpty() {
        UUID userId = USER_ID.get();
        return userId == null ? "" : userId.toString();
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_ID.remove();
    }
}
