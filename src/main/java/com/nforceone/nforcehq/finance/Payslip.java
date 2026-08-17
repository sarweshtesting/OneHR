package com.nforceone.nforcehq.finance;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payslips")
@Getter
@Setter
public class Payslip extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "gross_pay", nullable = false)
    private BigDecimal grossPay;

    @Column(nullable = false)
    private BigDecimal deductions;

    @Column(name = "net_pay", nullable = false)
    private BigDecimal netPay;

    @Column(nullable = false)
    private String status;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
