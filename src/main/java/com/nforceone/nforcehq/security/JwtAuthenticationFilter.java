package com.nforceone.nforcehq.security;

import com.nforceone.nforcehq.org.OrganizationRepository;
import com.nforceone.nforcehq.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the JWT on every request into a JwtPrincipal + the effective tenant for
 * that request, then clears TenantContext once the request completes — Tomcat reuses
 * worker threads across unrelated requests, so a stale tenant left behind here would
 * silently leak into whichever request runs on this thread next.
 *
 * Normal users (EMPLOYEE/MANAGER/ADMIN) always get the organization baked into their
 * token; any X-Organization-Id header they send is ignored. Only PLATFORM_ADMIN
 * tokens (which carry no fixed org) may select a tenant via that header, and only
 * after it's confirmed to be a real organization.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ORG_HEADER = "X-Organization-Id";

    private final JwtTokenProvider jwtTokenProvider;
    private final OrganizationRepository organizationRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (token != null) {
                authenticate(token, request);
            }
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            // Thrown only for a resolvable-but-invalid tenant selection (e.g. a
            // PLATFORM_ADMIN's X-Organization-Id header naming an org that doesn't
            // exist) — this runs before Spring Security's ExceptionTranslationFilter
            // in the chain, so it won't otherwise be turned into a clean 401.
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + ex.getMessage() + "\"}");
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(String token, HttpServletRequest request) {
        try {
            Jws<Claims> jws = jwtTokenProvider.parse(token);
            Claims claims = jws.getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String role = claims.get("role", String.class);
            String orgClaim = claims.get("org", String.class);
            UUID homeOrganizationId = orgClaim != null ? UUID.fromString(orgClaim) : null;

            UUID effectiveOrganizationId = resolveEffectiveTenant(request, role, homeOrganizationId);
            TenantContext.set(effectiveOrganizationId, userId);

            JwtPrincipal principal = new JwtPrincipal(
                    userId, homeOrganizationId, effectiveOrganizationId, role,
                    claims.get("name", String.class), claims.get("email", String.class));

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
        }
    }

    private UUID resolveEffectiveTenant(HttpServletRequest request, String role, UUID homeOrganizationId) {
        if (Role.PLATFORM_ADMIN.name().equals(role)) {
            String header = request.getHeader(ORG_HEADER);
            if (header == null || header.isBlank()) {
                return null;
            }
            UUID requested;
            try {
                requested = UUID.fromString(header);
            } catch (IllegalArgumentException ex) {
                throw new BadCredentialsException("Invalid " + ORG_HEADER + " header");
            }
            if (!organizationRepository.existsById(requested)) {
                throw new BadCredentialsException("Unknown organization: " + requested);
            }
            return requested;
        }
        return homeOrganizationId;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
