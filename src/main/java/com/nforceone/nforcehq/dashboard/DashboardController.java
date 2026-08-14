package com.nforceone.nforcehq.dashboard;

import com.nforceone.nforcehq.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ApprovalsService approvalsService;

    @GetMapping("/api/dashboard/stats")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','PLATFORM_ADMIN')")
    public DashboardStats stats(@AuthenticationPrincipal JwtPrincipal principal) {
        return dashboardService.stats(principal);
    }

    @GetMapping("/api/approvals/pending")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','PLATFORM_ADMIN')")
    public java.util.List<ApprovalItem> pendingApprovals(@AuthenticationPrincipal JwtPrincipal principal) {
        return approvalsService.pending(principal);
    }
}
