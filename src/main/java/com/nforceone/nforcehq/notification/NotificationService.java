package com.nforceone.nforcehq.notification;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationView> list(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        return notificationRepository.findByOrganizationIdAndUserIdOrderByCreatedAtDesc(organizationId, principal.userId())
                .stream().map(NotificationView::from).toList();
    }

    public long unreadCount(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        return notificationRepository.countByOrganizationIdAndUserIdAndReadFalse(organizationId, principal.userId());
    }

    @Transactional
    public NotificationView markRead(JwtPrincipal principal, UUID id) {
        UUID organizationId = requireOrganization(principal);
        Notification notification = notificationRepository
                .findByIdAndOrganizationIdAndUserId(id, organizationId, principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
        return NotificationView.from(notification);
    }

    @Transactional
    public void markAllRead(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<Notification> unread = notificationRepository
                .findByOrganizationIdAndUserIdAndReadFalse(organizationId, principal.userId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    /** Fire-and-forget notification creation used by leave/regularization services on submit/approve/reject. */
    @Transactional
    public void notify(UUID organizationId, UUID recipientUserId, String type, String title, String body, UUID relatedId) {
        if (recipientUserId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setOrganizationId(organizationId);
        notification.setUserId(recipientUserId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRelatedId(relatedId);
        notificationRepository.save(notification);
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
