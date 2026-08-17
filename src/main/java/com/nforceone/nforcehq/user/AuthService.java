package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.org.Organization;
import com.nforceone.nforcehq.org.OrganizationRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.security.JwtTokenProvider;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SLUG_RANDOM = new SecureRandom();

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("This account has been deactivated");
        }

        String token = jwtTokenProvider.generateToken(user);
        return new LoginResponse(token, toSummary(user));
    }

    /** Self-service workspace creation: signing up creates a brand-new organization and its first SUPER_ADMIN user. */
    @Transactional
    public LoginResponse signup(SignupRequest request) {
        String email = request.adminEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        Organization org = new Organization();
        org.setName(request.orgName().trim());
        org.setSlug(generateUniqueSlug(request.orgName()));
        org.setTimezone("UTC");
        organizationRepository.save(org);

        User user = new User();
        user.setOrganizationId(org.getId());
        user.setFullName(request.adminFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.SUPER_ADMIN);
        user.setAvatarInitials(initialsOf(request.adminFullName()));
        user.setActive(true);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return new LoginResponse(token, toSummary(user));
    }

    private String generateUniqueSlug(String orgName) {
        String base = orgName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        if (base.isBlank()) base = "workspace";
        String slug = base;
        while (organizationRepository.existsBySlug(slug)) {
            slug = base + "-" + (1000 + SLUG_RANDOM.nextInt(9000));
        }
        return slug;
    }

    private String initialsOf(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int i = 0; i < parts.length && initials.length() < 2; i++) {
            if (!parts[i].isEmpty()) initials.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return initials.toString();
    }

    public UserSummary me(JwtPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return toSummary(user);
    }

    private UserSummary toSummary(User user) {
        String orgName = null;
        if (user.getOrganizationId() != null) {
            orgName = organizationRepository.findById(user.getOrganizationId())
                    .map(Organization::getName)
                    .orElse(null);
        }
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(),
                user.getRole().name(), user.getJobTitle(), user.getOrganizationId(), orgName, AvatarUtil.dataUri(user));
    }
}
