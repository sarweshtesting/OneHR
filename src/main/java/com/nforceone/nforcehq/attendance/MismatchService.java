package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.common.ApprovalScope;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.HashMap;
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
public class MismatchService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MismatchView> openMismatches(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<UUID> userIds = ApprovalScope.resolve(principal, userRepository, organizationId);
        if (userIds.isEmpty()) {
            return List.of();
        }

        Query query = entityManager.createNativeQuery(
                "select user_id, work_date, internal_hours, client_name, client_hours, hours_delta "
                        + "from v_attendance_mismatches "
                        + "where organization_id = :orgId and user_id in :userIds "
                        + "and not exists (select 1 from attendance_mismatch_resolutions r "
                        + "  where r.organization_id = v_attendance_mismatches.organization_id "
                        + "  and r.user_id = v_attendance_mismatches.user_id "
                        + "  and r.work_date = v_attendance_mismatches.work_date) "
                        + "order by work_date desc");
        query.setParameter("orgId", organizationId);
        query.setParameter("userIds", userIds);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<UUID, String> namesById = new HashMap<>();
        userRepository.findAllById(Set.copyOf(userIds)).forEach(u -> namesById.put(u.getId(), u.getFullName()));

        return rows.stream().map(row -> {
            UUID userId = (UUID) row[0];
            LocalDate workDate = row[1] instanceof LocalDate ld ? ld : ((java.sql.Date) row[1]).toLocalDate();
            double internalHours = ((Number) row[2]).doubleValue();
            String clientName = (String) row[3];
            double clientHours = row[4] != null ? ((Number) row[4]).doubleValue() : 0;
            double hoursDelta = ((Number) row[5]).doubleValue();
            return new MismatchView(userId + "::" + workDate, userId, namesById.getOrDefault(userId, "Unknown"),
                    workDate, internalHours, clientName, clientHours, hoursDelta);
        }).toList();
    }

    @Transactional
    public void resolve(JwtPrincipal principal, String pseudoId) {
        UUID organizationId = requireOrganization(principal);
        String[] parts = pseudoId.split("::", 2);
        if (parts.length != 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid mismatch id");
        }
        UUID userId;
        LocalDate workDate;
        try {
            userId = UUID.fromString(parts[0]);
            workDate = LocalDate.parse(parts[1]);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid mismatch id");
        }

        Query query = entityManager.createNativeQuery(
                "insert into attendance_mismatch_resolutions (organization_id, user_id, work_date, resolved_by) "
                        + "values (:orgId, :userId, :workDate, :resolvedBy) on conflict do nothing");
        query.setParameter("orgId", organizationId);
        query.setParameter("userId", userId);
        query.setParameter("workDate", workDate);
        query.setParameter("resolvedBy", principal.userId());
        query.executeUpdate();
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
