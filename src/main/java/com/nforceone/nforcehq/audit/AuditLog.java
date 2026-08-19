package com.nforceone.nforcehq.audit;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Append-only — no updated_at, entries are never edited. */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLog extends TenantAwareEntity {

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String description;
}
