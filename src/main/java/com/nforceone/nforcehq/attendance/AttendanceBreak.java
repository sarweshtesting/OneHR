package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * No organization_id of its own — scoped indirectly through its parent
 * attendance_records row, per the RLS design in V1__init_schema.sql.
 */
@Entity
@Table(name = "attendance_breaks")
@Getter
@Setter
public class AttendanceBreak extends BaseEntity {

    @Column(name = "attendance_record_id", nullable = false)
    private UUID attendanceRecordId;

    @Column(name = "break_start_at", nullable = false)
    private Instant breakStartAt;

    @Column(name = "break_end_at")
    private Instant breakEndAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "break_type", nullable = false)
    private BreakType breakType = BreakType.OTHER;
}
