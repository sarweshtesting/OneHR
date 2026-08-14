package com.nforceone.nforcehq.leave;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {

    List<LeaveType> findByOrganizationIdAndActiveTrue(UUID organizationId);

    Optional<LeaveType> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
