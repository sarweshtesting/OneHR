package com.nforceone.nforcehq.attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByUserIdAndOrganizationIdAndWorkDate(
            UUID userId, UUID organizationId, LocalDate workDate);

    List<AttendanceRecord> findByOrganizationIdAndUserIdInAndWorkDate(
            UUID organizationId, List<UUID> userIds, LocalDate workDate);

    List<AttendanceRecord> findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(
            UUID organizationId, List<UUID> userIds, LocalDate start, LocalDate end);

    Page<AttendanceRecord> findByOrganizationIdAndUserIdInAndWorkDateBetweenOrderByWorkDateDesc(
            UUID organizationId, List<UUID> userIds, LocalDate start, LocalDate end, Pageable pageable);
}
