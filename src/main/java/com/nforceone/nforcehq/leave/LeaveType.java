package com.nforceone.nforcehq.leave;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
public class LeaveType extends TenantAwareEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private LeaveTypeCode code;

    @Column(nullable = false)
    private String name;

    @Column(name = "color_token")
    private String colorToken;

    @Column(name = "annual_quota_days", nullable = false)
    private BigDecimal annualQuotaDays;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
