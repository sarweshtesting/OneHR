package com.nforceone.nforcehq.leave;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {

    List<LeaveBalance> findByUserIdAndYear(UUID userId, int year);

    Optional<LeaveBalance> findByUserIdAndLeaveTypeIdAndYear(UUID userId, UUID leaveTypeId, int year);
}
