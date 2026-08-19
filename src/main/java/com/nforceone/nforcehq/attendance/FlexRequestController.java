package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attendance/flex-requests")
@RequiredArgsConstructor
public class FlexRequestController {

    private final FlexRequestService flexRequestService;

    @PostMapping
    public FlexRequestView submit(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody SubmitFlexRequestRequest request) {
        return flexRequestService.submit(principal, request);
    }

    @GetMapping("/me")
    public List<FlexRequestView> myRequests(@AuthenticationPrincipal JwtPrincipal principal) {
        return flexRequestService.myRequests(principal);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<FlexRequestView> pending(@AuthenticationPrincipal JwtPrincipal principal) {
        return flexRequestService.pending(principal);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public FlexRequestView approve(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return flexRequestService.approve(principal, id);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public FlexRequestView reject(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return flexRequestService.reject(principal, id);
    }
}
