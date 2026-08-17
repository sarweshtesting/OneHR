package com.nforceone.nforcehq.leave;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveAttachmentRepository extends JpaRepository<LeaveAttachment, UUID> {

    List<LeaveAttachment> findByOrganizationIdAndLeaveRequestIdOrderByCreatedAtAsc(UUID organizationId, UUID leaveRequestId);

    Optional<LeaveAttachment> findByIdAndOrganizationIdAndLeaveRequestId(UUID id, UUID organizationId, UUID leaveRequestId);
}
