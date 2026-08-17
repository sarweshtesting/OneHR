package com.nforceone.nforcehq.org;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

    List<Holiday> findByOrganizationIdOrderByHolidayDateAsc(UUID organizationId);
}
