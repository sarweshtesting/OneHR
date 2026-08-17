package com.nforceone.nforcehq.people;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record AddPersonRequest(
        @NotBlank String fullName,
        @NotBlank String email,
        @NotBlank String role,
        String jobTitle,
        UUID departmentId) {
}
