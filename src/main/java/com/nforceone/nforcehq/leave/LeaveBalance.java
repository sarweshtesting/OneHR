package com.nforceone.nforcehq.leave;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Deliberately does not extend BaseEntity/TenantAwareEntity: leave_balances has no
 * created_at column (only updated_at) and no organization_id of its own — it's scoped
 * indirectly through user_id, per the RLS design in V1__init_schema.sql.
 */
@Entity
@Table(name = "leave_balances")
@Getter
@Setter
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;

    @Column(nullable = false)
    private int year;

    @Column(name = "allocated_days", nullable = false)
    private BigDecimal allocatedDays;

    @Column(name = "used_days", nullable = false)
    private BigDecimal usedDays;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
