package com.nforceone.nforcehq.notification;

import com.nforceone.nforcehq.security.JwtPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationView> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return notificationService.list(principal);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal JwtPrincipal principal) {
        return new UnreadCountResponse(notificationService.unreadCount(principal));
    }

    @PostMapping("/{id}/read")
    public NotificationView markRead(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return notificationService.markRead(principal, id);
    }

    @PostMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal JwtPrincipal principal) {
        notificationService.markAllRead(principal);
    }
}
