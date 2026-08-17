package com.nforceone.nforcehq.people;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.Roles;
import com.nforceone.nforcehq.org.Department;
import com.nforceone.nforcehq.org.DepartmentRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plain EMPLOYEE viewers see everyone's name/title/department but not phone/email — MANAGER and above see full contact details. */
@RestController
@RequestMapping("/api/people")
@RequiredArgsConstructor
public class PeopleController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public List<PersonSummary> list(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        Map<UUID, String> departmentNames = departmentRepository.findByOrganizationId(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(Department::getId, Department::getName));

        boolean canSeeContactDetails = Roles.MANAGER_UP.contains(principal.role());

        return userRepository.findByOrganizationId(organizationId).stream()
                .filter(User::isActive)
                .map(u -> {
                    boolean revealContact = canSeeContactDetails || u.getId().equals(principal.userId());
                    return new PersonSummary(
                            u.getId(), u.getFullName(), revealContact ? u.getEmail() : null, revealContact ? u.getPhone() : null,
                            u.getRole().name(), u.getJobTitle(),
                            u.getDepartmentId() != null ? departmentNames.get(u.getDepartmentId()) : null,
                            u.getAvatarInitials());
                })
                .sorted(java.util.Comparator.comparing(PersonSummary::fullName))
                .toList();
    }
}
