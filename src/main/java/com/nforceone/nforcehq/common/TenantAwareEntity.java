package com.nforceone.nforcehq.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for every entity that carries an organization_id discriminator column,
 * mirrored server-side by a Postgres RLS policy on the same table.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
}
