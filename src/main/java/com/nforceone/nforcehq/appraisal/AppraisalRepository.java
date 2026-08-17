package com.nforceone.nforcehq.appraisal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppraisalRepository extends JpaRepository<Appraisal, UUID> {

    List<Appraisal> findByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, UUID userId);

    List<Appraisal> findByOrganizationIdAndUserIdInOrderByCreatedAtDesc(UUID organizationId, List<UUID> userIds);

    Optional<Appraisal> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
