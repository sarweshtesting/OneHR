package com.nforceone.nforcehq.people;

import java.util.UUID;

/** temporaryPassword is returned exactly once — it is not recoverable after this response. */
public record AddPersonResponse(UUID id, String fullName, String email, String role, String temporaryPassword) {
}
