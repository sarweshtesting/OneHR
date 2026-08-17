package com.nforceone.nforcehq.appraisal;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.ApprovalScope;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppraisalService {

    private final AppraisalRepository appraisalRepository;
    private final UserRepository userRepository;

    public List<AppraisalView> mine(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<Appraisal> appraisals = appraisalRepository
                .findByOrganizationIdAndUserIdOrderByCreatedAtDesc(organizationId, principal.userId());
        return toViews(appraisals);
    }

    public List<AppraisalView> team(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        return toViews(appraisalRepository.findByOrganizationIdAndUserIdInOrderByCreatedAtDesc(organizationId, userIds));
    }

    @Transactional
    public AppraisalView create(JwtPrincipal principal, CreateAppraisalRequest request) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> scope = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (!scope.contains(request.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This employee is outside your review scope");
        }

        Appraisal appraisal = new Appraisal();
        appraisal.setOrganizationId(organizationId);
        appraisal.setUserId(request.userId());
        appraisal.setReviewerId(principal.userId());
        appraisal.setCycleName(request.cycleName());
        appraisal.setOverallRating(request.overallRating());
        appraisal.setStrengths(request.strengths());
        appraisal.setAreasForImprovement(request.areasForImprovement());
        appraisal.setGoalsNextCycle(request.goalsNextCycle());
        appraisal.setStatus("SUBMITTED");
        appraisalRepository.save(appraisal);

        return toView(appraisal, namesFor(List.of(appraisal.getUserId(), appraisal.getReviewerId())));
    }

    @Transactional
    public AppraisalView acknowledge(JwtPrincipal principal, UUID id) {
        UUID organizationId = requireOrganization(principal);
        Appraisal appraisal = appraisalRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Appraisal not found"));
        if (!appraisal.getUserId().equals(principal.userId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only acknowledge your own appraisal");
        }
        appraisal.setStatus("ACKNOWLEDGED");
        appraisalRepository.save(appraisal);
        return toView(appraisal, namesFor(List.of(appraisal.getUserId(), appraisal.getReviewerId())));
    }

    private List<AppraisalView> toViews(List<Appraisal> appraisals) {
        Set<UUID> ids = new java.util.HashSet<>();
        appraisals.forEach(a -> {
            ids.add(a.getUserId());
            if (a.getReviewerId() != null) ids.add(a.getReviewerId());
        });
        Map<UUID, String> names = namesFor(List.copyOf(ids));
        return appraisals.stream().map(a -> toView(a, names)).toList();
    }

    private AppraisalView toView(Appraisal a, Map<UUID, String> names) {
        return new AppraisalView(
                a.getId(), a.getUserId(), names.getOrDefault(a.getUserId(), "Unknown"),
                a.getReviewerId() != null ? names.get(a.getReviewerId()) : null,
                a.getCycleName(), a.getOverallRating(), a.getStrengths(), a.getAreasForImprovement(),
                a.getGoalsNextCycle(), a.getStatus(), a.getCreatedAt());
    }

    private Map<UUID, String> namesFor(List<UUID> ids) {
        List<UUID> nonNull = ids.stream().filter(java.util.Objects::nonNull).toList();
        if (nonNull.isEmpty()) return Map.of();
        return userRepository.findAllById(Set.copyOf(nonNull)).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName));
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
