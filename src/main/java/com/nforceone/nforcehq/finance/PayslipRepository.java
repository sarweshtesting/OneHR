package com.nforceone.nforcehq.finance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayslipRepository extends JpaRepository<Payslip, UUID> {

    List<Payslip> findByUserIdOrderByPeriodMonthDesc(UUID userId);

    List<Payslip> findByOrganizationIdOrderByPeriodMonthDescUserIdAsc(UUID organizationId);
}
