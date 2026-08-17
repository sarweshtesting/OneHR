package com.nforceone.nforcehq.people;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.org.Department;
import com.nforceone.nforcehq.org.DepartmentRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/people")
@RequiredArgsConstructor
public class PeopleController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public java.util.List<PersonSummary> list(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        Map<UUID, String> departmentNames = departmentRepository.findByOrganizationId(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(Department::getId, Department::getName));

        return userRepository.findByOrganizationId(organizationId).stream()
                .filter(User::isActive)
                .map(u -> new PersonSummary(
                        u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().name(),
                        u.getJobTitle(),
                        u.getDepartmentId() != null ? departmentNames.get(u.getDepartmentId()) : null,
                        u.getAvatarInitials()))
                .sorted(java.util.Comparator.comparing(PersonSummary::fullName))
                .toList();
    }
}
