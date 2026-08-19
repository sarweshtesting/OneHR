package com.nforceone.nforcehq.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceFlexRequestRepository extends JpaRepository<AttendanceFlexRequest, UUID> {

    List<AttendanceFlexRequest> findByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, UUID userId);

    List<AttendanceFlexRequest> findByOrganizationIdAndUserIdInAndStatusOrderByCreatedAtAsc(
            UUID organizationId, List<UUID> userIds, RegularizationStatus status);

    Optional<AttendanceFlexRequest> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
