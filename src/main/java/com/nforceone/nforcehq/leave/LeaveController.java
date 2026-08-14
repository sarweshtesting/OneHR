package com.nforceone.nforcehq.leave;

import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping("/api/leave-types")
    public List<LeaveTypeSummary> leaveTypes(@AuthenticationPrincipal JwtPrincipal principal) {
        return leaveService.listTypes(principal);
    }

    @GetMapping("/api/leave/balances/me")
    public List<LeaveBalanceView> myBalances(@AuthenticationPrincipal JwtPrincipal principal) {
        return leaveService.balances(principal);
    }

    @PostMapping("/api/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestView apply(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody ApplyLeaveRequest request) {
        return leaveService.apply(principal, request);
    }

    @GetMapping("/api/me/requests/history")
    public List<RequestHistoryItem> history(@AuthenticationPrincipal JwtPrincipal principal) {
        return leaveService.history(principal);
    }

    @GetMapping("/api/leave/team-calendar")
    public List<TeamCalendarEntry> teamCalendar(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(defaultValue = "30") int range) {
        return leaveService.teamCalendar(principal, range);
    }

    @PostMapping("/api/leave-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','PLATFORM_ADMIN')")
    public LeaveRequestView approveLeave(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return leaveService.approve(principal, id);
    }

    @PostMapping("/api/leave-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','PLATFORM_ADMIN')")
    public LeaveRequestView rejectLeave(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return leaveService.reject(principal, id);
    }
}
