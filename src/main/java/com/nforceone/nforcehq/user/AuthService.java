package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.org.Organization;
import com.nforceone.nforcehq.org.OrganizationRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtTokenProvider jwtTokenProvider;

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
                user.getRole().name(), user.getJobTitle(), user.getOrganizationId(), orgName);
    }
}
