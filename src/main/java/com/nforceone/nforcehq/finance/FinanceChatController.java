package com.nforceone.nforcehq.finance;

import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/finance/chat")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','HR_ADMIN','ADMIN','SUPER_ADMIN','PLATFORM_ADMIN')")
public class FinanceChatController {

    private final FinanceChatService financeChatService;

    @GetMapping("/messages")
    public List<FinanceChatMessageView> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return financeChatService.list(principal);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public FinanceChatMessageView send(@AuthenticationPrincipal JwtPrincipal principal, @Valid @RequestBody SendChatMessageRequest request) {
        return financeChatService.send(principal, request);
    }
}
