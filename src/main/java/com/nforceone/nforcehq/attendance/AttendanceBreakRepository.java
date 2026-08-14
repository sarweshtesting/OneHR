package com.nforceone.nforcehq.attendance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceBreakRepository extends JpaRepository<AttendanceBreak, UUID> {

    List<AttendanceBreak> findByAttendanceRecordIdOrderByBreakStartAtAsc(UUID attendanceRecordId);

    Optional<AttendanceBreak> findFirstByAttendanceRecordIdAndBreakEndAtIsNull(UUID attendanceRecordId);

    List<AttendanceBreak> findByAttendanceRecordIdInAndBreakEndAtIsNull(List<UUID> attendanceRecordIds);
}
