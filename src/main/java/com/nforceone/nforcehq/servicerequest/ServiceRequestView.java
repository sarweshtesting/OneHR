package com.nforceone.nforcehq.servicerequest;

import java.time.Instant;
import java.util.UUID;

public record ServiceRequestView(
        UUID id, UUID requesterId, String requesterName, ServiceRequestType type, String subject, String description,
        ServiceRequestStatus status, UUID assigneeId, String assigneeName, String resolutionNotes,
        Instant createdAt, Instant updatedAt) {

    public static ServiceRequestView from(ServiceRequest r, String requesterName, String assigneeName) {
        return new ServiceRequestView(r.getId(), r.getRequesterId(), requesterName, r.getType(), r.getSubject(),
                r.getDescription(), r.getStatus(), r.getAssigneeId(), assigneeName, r.getResolutionNotes(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
