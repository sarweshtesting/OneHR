package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.security.JwtPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/regularizations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
public class RegularizationController {

    private final RegularizationService regularizationService;

    @PostMapping("/{id}/approve")
    public RegularizationView approve(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return regularizationService.approve(principal, id);
    }

    @PostMapping("/{id}/reject")
    public RegularizationView reject(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return regularizationService.reject(principal, id);
    }
}
