package com.nforceone.nforcehq.servicerequest;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "service_requests")
@Getter
@Setter
public class ServiceRequest extends TenantAwareEntity {

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ServiceRequestType type;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ServiceRequestStatus status = ServiceRequestStatus.OPEN;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;
}
