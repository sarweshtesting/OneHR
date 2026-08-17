package com.nforceone.nforcehq.people;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.Roles;
import com.nforceone.nforcehq.org.Department;
import com.nforceone.nforcehq.org.DepartmentRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.Role;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Plain EMPLOYEE viewers see everyone's name/title/department but not phone/email — MANAGER and above see full contact details. */
@RestController
@RequestMapping("/api/people")
@RequiredArgsConstructor
public class PeopleController {

    private static final SecureRandom TEMP_PASSWORD_RANDOM = new SecureRandom();
    private static final Set<String> HR_ADMIN_CREATABLE_ROLES = Set.of("EMPLOYEE", "MANAGER");
    private static final Set<String> SUPER_ADMIN_CREATABLE_ROLES = Set.of("EMPLOYEE", "MANAGER", "HR_ADMIN");

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

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

    /**
     * HR_ADMIN may only create EMPLOYEE/MANAGER; SUPER_ADMIN (and PLATFORM_ADMIN acting on
     * a selected org) may additionally create HR_ADMIN — nobody can create SUPER_ADMIN or
     * PLATFORM_ADMIN through this endpoint, so privilege escalation isn't possible via invite.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    @Transactional
    public AddPersonResponse addPerson(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody AddPersonRequest request) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }

        String requestedRole = request.role().trim().toUpperCase();
        Set<String> allowedRoles = "HR_ADMIN".equals(principal.role()) ? HR_ADMIN_CREATABLE_ROLES : SUPER_ADMIN_CREATABLE_ROLES;
        if (!allowedRoles.contains(requestedRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can't assign the role " + requestedRole);
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        if (request.departmentId() != null
                && departmentRepository.findById(request.departmentId())
                        .filter(d -> d.getOrganizationId().equals(organizationId))
                        .isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown department");
        }

        String temporaryPassword = generateTemporaryPassword();

        User user = new User();
        user.setOrganizationId(organizationId);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setRole(Role.valueOf(requestedRole));
        user.setJobTitle(request.jobTitle());
        user.setDepartmentId(request.departmentId());
        user.setAvatarInitials(initialsOf(request.fullName()));
        user.setActive(true);
        userRepository.save(user);

        return new AddPersonResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(), temporaryPassword);
    }

    private static String generateTemporaryPassword() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(alphabet.charAt(TEMP_PASSWORD_RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static String initialsOf(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length && initials.length() < 2; i++) {
            if (!parts[i].isEmpty()) initials.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return initials.toString();
    }
}
