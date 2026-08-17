package com.nforceone.nforcehq.user;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank String email) {
}
