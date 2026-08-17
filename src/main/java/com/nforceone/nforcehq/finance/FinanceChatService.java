package com.nforceone.nforcehq.finance;

import com.nforceone.nforcehq.common.ApiException;
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
public class FinanceChatService {

    private final FinanceChatMessageRepository financeChatMessageRepository;
    private final UserRepository userRepository;

    public List<FinanceChatMessageView> list(JwtPrincipal principal) {
        UUID organizationId = requireOrganization(principal);
        List<FinanceChatMessage> messages = financeChatMessageRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId);
        Map<UUID, String> names = userRepository
                .findAllById(Set.copyOf(messages.stream().map(FinanceChatMessage::getUserId).toList()))
                .stream().collect(java.util.stream.Collectors.toMap(User::getId, User::getFullName));
        return messages.stream()
                .map(m -> new FinanceChatMessageView(m.getId(), m.getUserId(),
                        names.getOrDefault(m.getUserId(), "Unknown"), m.getMessage(), m.getCreatedAt()))
                .toList();
    }

    @Transactional
    public FinanceChatMessageView send(JwtPrincipal principal, SendChatMessageRequest request) {
        UUID organizationId = requireOrganization(principal);
        FinanceChatMessage message = new FinanceChatMessage();
        message.setOrganizationId(organizationId);
        message.setUserId(principal.userId());
        message.setMessage(request.message());
        financeChatMessageRepository.save(message);
        return new FinanceChatMessageView(message.getId(), message.getUserId(), principal.name(), message.getMessage(), message.getCreatedAt());
    }

    private UUID requireOrganization(JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        return organizationId;
    }
}
