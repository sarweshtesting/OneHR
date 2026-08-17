package com.nforceone.nforcehq.notification;

import java.time.Instant;
import java.util.UUID;

/** type is one of LEAVE_SUBMITTED, LEAVE_APPROVED, LEAVE_REJECTED, REGULARIZATION_SUBMITTED, REGULARIZATION_APPROVED, REGULARIZATION_REJECTED. */
public record NotificationView(
        UUID id, String type, String title, String body, UUID relatedId, boolean read, Instant createdAt) {

    static NotificationView from(Notification notification) {
        return new NotificationView(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getRelatedId(), notification.isRead(), notification.getCreatedAt());
    }
}
