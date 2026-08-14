package com.nforceone.nforcehq.org;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    List<Department> findByOrganizationId(UUID organizationId);
}
