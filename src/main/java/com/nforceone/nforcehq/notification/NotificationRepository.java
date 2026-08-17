package com.nforceone.nforcehq.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, UUID userId);

    Optional<Notification> findByIdAndOrganizationIdAndUserId(UUID id, UUID organizationId, UUID userId);

    long countByOrganizationIdAndUserIdAndReadFalse(UUID organizationId, UUID userId);

    List<Notification> findByOrganizationIdAndUserIdAndReadFalse(UUID organizationId, UUID userId);
}
