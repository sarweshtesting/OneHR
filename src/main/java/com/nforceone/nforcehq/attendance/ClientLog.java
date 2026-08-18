package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "client_logs")
@Getter
@Setter
public class ClientLog extends TenantAwareEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "logged_hours", nullable = false)
    private BigDecimal loggedHours;

    private String source;
}
