package com.nforceone.nforcehq.user;

import java.time.LocalDate;
import java.util.UUID;

public record ProfileView(
        UUID id,
        String fullName,
        String email,
        String role,
        String phone,
        LocalDate dateOfBirth,
        String bloodGroup,
        String employeeCode,
        String jobTitle,
        String departmentName,
        String managerName,
        LocalDate hireDate,
        String avatarInitials,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhone) {
}
