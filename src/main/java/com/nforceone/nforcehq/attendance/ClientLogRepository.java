package com.nforceone.nforcehq.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientLogRepository extends JpaRepository<ClientLog, UUID> {

    List<ClientLog> findByUserIdOrderByWorkDateDesc(UUID userId);

    List<ClientLog> findByOrganizationIdOrderByWorkDateDescUserIdAsc(UUID organizationId);

    List<ClientLog> findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(UUID userId, LocalDate from, LocalDate to);

    Optional<ClientLog> findByUserIdAndWorkDateAndClientIdAndSource(UUID userId, LocalDate workDate, UUID clientId, String source);
}
