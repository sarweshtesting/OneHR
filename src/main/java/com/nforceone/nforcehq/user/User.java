package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * organization_id is nullable only for PLATFORM_ADMIN users (nforceone.com staff who
 * operate across multiple client organizations) — enforced by a DB check constraint,
 * not by JPA. Department/manager relations are kept as plain UUID columns rather than
 * JPA associations to avoid incidental lazy-loading complexity in this phase.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Role role;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "avatar_initials")
    private String avatarInitials;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "blood_group")
    private String bloodGroup;

    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_relationship")
    private String emergencyContactRelationship;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;
}
