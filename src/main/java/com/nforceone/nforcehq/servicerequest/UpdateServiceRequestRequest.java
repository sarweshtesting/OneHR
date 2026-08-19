package com.nforceone.nforcehq.servicerequest;

import java.util.UUID;

/** Every field is optional — the caller sends only what's changing. */
public record UpdateServiceRequestRequest(
        ServiceRequestStatus status, UUID assigneeId, String resolutionNotes) {
}
