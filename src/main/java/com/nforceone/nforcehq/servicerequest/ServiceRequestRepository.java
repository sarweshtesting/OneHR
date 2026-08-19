package com.nforceone.nforcehq.servicerequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByOrganizationIdAndRequesterIdOrderByCreatedAtDesc(UUID organizationId, UUID requesterId);

    List<ServiceRequest> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<ServiceRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
