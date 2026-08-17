package com.nforceone.nforcehq.finance;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipRepository payslipRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public List<PayslipView> mine(@AuthenticationPrincipal JwtPrincipal principal) {
        User self = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return payslipRepository.findByUserIdOrderByPeriodMonthDesc(principal.userId()).stream()
                .map(p -> toView(p, self))
                .toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<PayslipView> organizationWide(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        Map<UUID, User> usersById = userRepository.findByOrganizationId(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        return payslipRepository.findByOrganizationIdOrderByPeriodMonthDescUserIdAsc(organizationId).stream()
                .map(p -> toView(p, usersById.get(p.getUserId())))
                .toList();
    }

    private PayslipView toView(Payslip p, User user) {
        return new PayslipView(
                p.getId(), p.getUserId(),
                user != null ? user.getFullName() : "Former employee",
                user != null ? user.getAvatarInitials() : "?",
                p.getPeriodMonth(), p.getGrossPay(), p.getDeductions(), p.getNetPay(), p.getStatus());
    }
}
