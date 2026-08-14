package com.nforceone.nforcehq.org;

import com.nforceone.nforcehq.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "departments")
@Getter
@Setter
public class Department extends TenantAwareEntity {

    @Column(nullable = false)
    private String name;
}
