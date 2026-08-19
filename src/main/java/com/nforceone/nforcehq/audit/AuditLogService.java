package com.nforceone.nforcehq.audit;

import com.nforceone.nforcehq.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(JwtPrincipal principal, String action, String description) {
        if (principal.effectiveOrganizationId() == null) {
            return;
        }
        AuditLog log = new AuditLog();
        log.setOrganizationId(principal.effectiveOrganizationId());
        log.setActorUserId(principal.userId());
        log.setActorName(principal.name());
        log.setAction(action);
        log.setDescription(description);
        auditLogRepository.save(log);
    }
}
