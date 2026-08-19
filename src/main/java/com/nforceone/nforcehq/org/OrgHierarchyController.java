package com.nforceone.nforcehq.org;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.Roles;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MANAGER and above see the full company tree. Plain EMPLOYEE gets a shallow slice —
 * themselves, their manager, their manager's other direct reports, and their own
 * direct reports (if any) — rather than the whole organization's chart.
 */
@RestController
@RequestMapping("/api/org/hierarchy")
@RequiredArgsConstructor
public class OrgHierarchyController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @GetMapping
    public List<OrgHierarchyNode> hierarchy(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        List<User> all = userRepository.findByOrganizationId(organizationId).stream()
                .filter(User::isActive)
                .toList();
        Map<UUID, String> departmentNames = departmentRepository.findByOrganizationId(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(Department::getId, Department::getName));

        if (Roles.MANAGER_UP.contains(principal.role())) {
            return all.stream().map(u -> toNode(u, departmentNames)).toList();
        }

        Set<UUID> visible = new HashSet<>();
        visible.add(principal.userId());
        User self = all.stream().filter(u -> u.getId().equals(principal.userId())).findFirst().orElse(null);
        if (self != null && self.getManagerId() != null) {
            visible.add(self.getManagerId());
            all.stream().filter(u -> self.getManagerId().equals(u.getManagerId())).forEach(u -> visible.add(u.getId()));
        }
        all.stream().filter(u -> principal.userId().equals(u.getManagerId())).forEach(u -> visible.add(u.getId()));

        return all.stream().filter(u -> visible.contains(u.getId())).map(u -> toNode(u, departmentNames)).toList();
    }

    private OrgHierarchyNode toNode(User u, Map<UUID, String> departmentNames) {
        return new OrgHierarchyNode(u.getId(), u.getFullName(), u.getJobTitle(), u.getRole().name(), u.getAvatarInitials(), u.getManagerId(),
                u.getDepartmentId(), u.getDepartmentId() != null ? departmentNames.get(u.getDepartmentId()) : null);
    }
}
