package com.nforceone.nforcehq.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findTop200ByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
