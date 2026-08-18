package com.nforceone.nforcehq.attendance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientLogRepository extends JpaRepository<ClientLog, UUID> {

    List<ClientLog> findByUserIdOrderByWorkDateDesc(UUID userId);

    List<ClientLog> findByOrganizationIdOrderByWorkDateDescUserIdAsc(UUID organizationId);
}
