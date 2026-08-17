package com.nforceone.nforcehq.user;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Only personal/emergency-contact fields are self-editable — employment fields (job title, department, manager) are employer-managed. */
public record UpdateProfileRequest(
        @Size(max = 30) String phone,
        LocalDate dateOfBirth,
        @Size(max = 10) String bloodGroup,
        @Size(max = 200) String emergencyContactName,
        @Size(max = 100) String emergencyContactRelationship,
        @Size(max = 30) String emergencyContactPhone) {
}
