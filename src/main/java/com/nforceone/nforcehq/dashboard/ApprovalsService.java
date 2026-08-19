package com.nforceone.nforcehq.dashboard;

import com.nforceone.nforcehq.attendance.MismatchService;
import com.nforceone.nforcehq.attendance.RegularizationService;
import com.nforceone.nforcehq.leave.LeaveService;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApprovalsService {

    private final LeaveService leaveService;
    private final RegularizationService regularizationService;
    private final MismatchService mismatchService;

    public List<ApprovalItem> pending(JwtPrincipal principal) {
        List<ApprovalItem> items = new ArrayList<>();

        leaveService.pending(principal).forEach(r -> items.add(new ApprovalItem(
                "LEAVE", r.id().toString(), r.userName(),
                r.userName() + " — " + r.leaveTypeName() + ", " + r.daysRequested() + " day(s)",
                "Requested " + r.startDate() + (r.startDate().equals(r.endDate()) ? "" : " – " + r.endDate()))));

        regularizationService.pending(principal).forEach(r -> items.add(new ApprovalItem(
                "REGULARIZATION", r.id().toString(), r.userName(),
                r.userName() + " — Regularization",
                r.workDate() + (r.reason() != null && !r.reason().isBlank() ? " · " + r.reason() : ""))));

        mismatchService.openMismatches(principal).forEach(m -> items.add(new ApprovalItem(
                "MISMATCH", m.pseudoId(), m.userName(),
                "Attendance mismatch — " + m.userName(),
                "Client log: " + m.clientHours() + "h · Internal log: " + m.internalHours() + "h · " + m.workDate())));

        return items;
    }

    /** Same three request types, scoped to an explicit user list instead of the caller's
     * role-based approval scope — used by "My Team" (direct reports only, regardless of
     * whether the caller's role would otherwise see the whole organization). */
    public List<ApprovalItem> pendingForUsers(UUID organizationId, List<UUID> userIds) {
        List<ApprovalItem> items = new ArrayList<>();
        if (userIds.isEmpty()) {
            return items;
        }

        leaveService.pendingFor(organizationId, userIds).forEach(r -> items.add(new ApprovalItem(
                "LEAVE", r.id().toString(), r.userName(),
                r.userName() + " — " + r.leaveTypeName() + ", " + r.daysRequested() + " day(s)",
                "Requested " + r.startDate() + (r.startDate().equals(r.endDate()) ? "" : " – " + r.endDate()))));

        regularizationService.pendingFor(organizationId, userIds).forEach(r -> items.add(new ApprovalItem(
                "REGULARIZATION", r.id().toString(), r.userName(),
                r.userName() + " — Regularization",
                r.workDate() + (r.reason() != null && !r.reason().isBlank() ? " · " + r.reason() : ""))));

        mismatchService.openMismatchesFor(organizationId, userIds).forEach(m -> items.add(new ApprovalItem(
                "MISMATCH", m.pseudoId(), m.userName(),
                "Attendance mismatch — " + m.userName(),
                "Client log: " + m.clientHours() + "h · Internal log: " + m.internalHours() + "h · " + m.workDate())));

        return items;
    }
}
