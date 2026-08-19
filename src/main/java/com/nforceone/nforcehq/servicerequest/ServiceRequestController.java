package com.nforceone.nforcehq.servicerequest;

import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @PostMapping
    public ServiceRequestView submit(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody SubmitServiceRequestRequest request) {
        return serviceRequestService.submit(principal, request);
    }

    @GetMapping("/me")
    public List<ServiceRequestView> myRequests(@AuthenticationPrincipal JwtPrincipal principal) {
        return serviceRequestService.myRequests(principal);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<ServiceRequestView> inbox(@AuthenticationPrincipal JwtPrincipal principal) {
        return serviceRequestService.inbox(principal);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public ServiceRequestView update(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id,
            @RequestBody UpdateServiceRequestRequest request) {
        return serviceRequestService.update(principal, id, request);
    }
}
