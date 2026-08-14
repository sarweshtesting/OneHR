package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attendance_regularization_requests")
@Getter
@Setter
public class AttendanceRegularizationRequest extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "requested_clock_in")
    private Instant requestedClockIn;

    @Column(name = "requested_clock_out")
    private Instant requestedClockOut;

    @Column(columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private RegularizationStatus status = RegularizationStatus.PENDING;

    @Column(name = "approver_id")
    private UUID approverId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "resulting_attendance_record_id")
    private UUID resultingAttendanceRecordId;
}
