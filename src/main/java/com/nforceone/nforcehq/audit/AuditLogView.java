package com.nforceone.nforcehq.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogView(UUID id, String actorName, String action, String description, Instant createdAt) {

    static AuditLogView from(AuditLog log) {
        return new AuditLogView(log.getId(), log.getActorName(), log.getAction(), log.getDescription(), log.getCreatedAt());
    }
}
