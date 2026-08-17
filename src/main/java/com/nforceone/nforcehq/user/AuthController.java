package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal JwtPrincipal principal) {
        return authService.me(principal);
    }
}
