package com.nforceone.nforcehq.dashboard;

import com.nforceone.nforcehq.attendance.MismatchService;
import com.nforceone.nforcehq.attendance.RegularizationService;
import com.nforceone.nforcehq.leave.LeaveService;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.util.ArrayList;
import java.util.List;
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
}
