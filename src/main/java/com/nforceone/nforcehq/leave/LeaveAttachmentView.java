package com.nforceone.nforcehq.leave;

import java.time.Instant;
import java.util.UUID;

public record LeaveAttachmentView(UUID id, String fileName, String contentType, int fileSize, Instant createdAt) {

    static LeaveAttachmentView from(LeaveAttachment a) {
        return new LeaveAttachmentView(a.getId(), a.getFileName(), a.getContentType(), a.getFileSize(), a.getCreatedAt());
    }
}
