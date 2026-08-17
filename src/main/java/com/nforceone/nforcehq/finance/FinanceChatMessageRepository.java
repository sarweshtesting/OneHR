package com.nforceone.nforcehq.finance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceChatMessageRepository extends JpaRepository<FinanceChatMessage, UUID> {

    List<FinanceChatMessage> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);
}
