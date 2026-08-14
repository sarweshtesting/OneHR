package com.nforceone.nforcehq.org;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Powers the org-switcher dropdown for PLATFORM_ADMIN users (nforceone.com staff).
 * Every other role only ever sees their own organization, via /api/auth/me.
 */
@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationRepository organizationRepository;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<OrganizationSummary> list() {
        return organizationRepository.findAll().stream()
                .map(OrganizationSummary::from)
                .toList();
    }
}
