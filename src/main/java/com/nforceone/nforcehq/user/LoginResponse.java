package com.nforceone.nforcehq.user;

public record LoginResponse(String token, UserSummary user) {
}
