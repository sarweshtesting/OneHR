package com.nforceone.nforcehq.org;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org/hierarchy")
@RequiredArgsConstructor
public class OrgHierarchyController {

    private final UserRepository userRepository;

    @GetMapping
    public List<OrgHierarchyNode> hierarchy(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return userRepository.findByOrganizationId(organizationId).stream()
                .filter(User::isActive)
                .map(u -> new OrgHierarchyNode(u.getId(), u.getFullName(), u.getJobTitle(),
                        u.getRole().name(), u.getAvatarInitials(), u.getManagerId()))
                .toList();
    }
}
