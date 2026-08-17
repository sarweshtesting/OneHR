package com.nforceone.nforcehq.appraisal;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appraisals")
@RequiredArgsConstructor
public class AppraisalController {

    private final AppraisalService appraisalService;

    @GetMapping("/me")
    public List<AppraisalView> mine(@AuthenticationPrincipal JwtPrincipal principal) {
        return appraisalService.mine(principal);
    }

    @GetMapping("/team")
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<AppraisalView> team(@AuthenticationPrincipal JwtPrincipal principal) {
        return appraisalService.team(principal);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppraisalView create(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody CreateAppraisalRequest request) {
        return appraisalService.create(principal, request);
    }

    @PostMapping("/{id}/acknowledge")
    public AppraisalView acknowledge(@AuthenticationPrincipal JwtPrincipal principal, @PathVariable UUID id) {
        return appraisalService.acknowledge(principal, id);
    }
}
