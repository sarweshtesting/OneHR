package com.nforceone.nforcehq.org;

import com.nforceone.nforcehq.audit.AuditLogService;
import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<OrganizationSummary> list() {
        return organizationRepository.findAll().stream()
                .map(OrganizationSummary::from)
                .toList();
    }

    public record UpdateOrganizationRequest(@NotBlank String name) {
    }

    @PatchMapping("/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','PLATFORM_ADMIN')")
    public OrganizationSummary updateCurrent(@AuthenticationPrincipal JwtPrincipal principal, @RequestBody UpdateOrganizationRequest request) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Organization not found"));

        String oldName = organization.getName();
        String newName = request.name().trim();
        if (newName.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Organization name is required");
        }
        organization.setName(newName);
        organizationRepository.save(organization);

        auditLogService.record(principal, "ORG_RENAMED", principal.name() + " renamed the organization from \"" + oldName + "\" to \"" + newName + "\"");

        return OrganizationSummary.from(organization);
    }
}
