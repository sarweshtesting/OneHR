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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    public List<DepartmentSummary> list(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return departmentRepository.findByOrganizationId(organizationId).stream()
                .map(DepartmentSummary::from)
                .toList();
    }

    public record CreateDepartmentRequest(@NotBlank String name) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public DepartmentSummary create(@AuthenticationPrincipal JwtPrincipal principal, @RequestBody CreateDepartmentRequest request) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        String name = request.name().trim();
        if (name.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Department name is required");
        }
        if (departmentRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, name).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A department with this name already exists");
        }

        Department department = new Department();
        department.setOrganizationId(organizationId);
        department.setName(name);
        departmentRepository.save(department);

        auditLogService.record(principal, "DEPARTMENT_CREATED", principal.name() + " created department \"" + name + "\"");

        return DepartmentSummary.from(department);
    }
}
