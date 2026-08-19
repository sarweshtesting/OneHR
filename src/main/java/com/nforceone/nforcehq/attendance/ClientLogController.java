package com.nforceone.nforcehq.attendance;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.security.JwtPrincipal;
import com.nforceone.nforcehq.user.User;
import com.nforceone.nforcehq.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-logs")
@RequiredArgsConstructor
public class ClientLogController {

    private final ClientLogRepository clientLogRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientLogView submit(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody SubmitClientLogRequest request) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        User self = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Client client = resolveClient(organizationId, request);

        ClientLog log = new ClientLog();
        log.setOrganizationId(organizationId);
        log.setUserId(principal.userId());
        log.setWorkDate(request.workDate());
        log.setClientId(client.getId());
        log.setClientName(client.getName());
        log.setLoggedHours(request.loggedHours());
        log.setSource("MANUAL");
        clientLogRepository.save(log);

        return toView(log, self);
    }

    /** Either resolves the given clientId to an existing client, or creates one from
     * the inline newClientName/newClientContact/newClientNotes fields. */
    private Client resolveClient(UUID organizationId, SubmitClientLogRequest request) {
        if (request.clientId() != null) {
            Client client = clientRepository.findById(request.clientId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown client"));
            if (!client.getOrganizationId().equals(organizationId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown client");
            }
            return client;
        }
        if (request.newClientName() == null || request.newClientName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select a client or enter a new client's name");
        }
        String name = request.newClientName().trim();
        return clientRepository.findByOrganizationIdAndNameIgnoreCase(organizationId, name)
                .orElseGet(() -> {
                    Client client = new Client();
                    client.setOrganizationId(organizationId);
                    client.setName(name);
                    client.setContactPerson(blankToNull(request.newClientContact()));
                    client.setNotes(blankToNull(request.newClientNotes()));
                    return clientRepository.save(client);
                });
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    @GetMapping("/me")
    public List<ClientLogView> mine(@AuthenticationPrincipal JwtPrincipal principal) {
        User self = userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return clientLogRepository.findByUserIdOrderByWorkDateDesc(principal.userId()).stream()
                .map(l -> toView(l, self))
                .toList();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
    public List<ClientLogView> organizationWide(@AuthenticationPrincipal JwtPrincipal principal) {
        UUID organizationId = principal.effectiveOrganizationId();
        if (organizationId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Select an organization first");
        }
        Map<UUID, User> usersById = userRepository.findByOrganizationId(organizationId).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

        return clientLogRepository.findByOrganizationIdOrderByWorkDateDescUserIdAsc(organizationId).stream()
                .map(l -> toView(l, usersById.get(l.getUserId())))
                .toList();
    }

    private ClientLogView toView(ClientLog log, User user) {
        return new ClientLogView(
                log.getId(), log.getUserId(),
                user != null ? user.getFullName() : "Former employee",
                user != null ? user.getAvatarInitials() : "?",
                log.getWorkDate(), log.getClientName(), log.getLoggedHours());
    }
}
