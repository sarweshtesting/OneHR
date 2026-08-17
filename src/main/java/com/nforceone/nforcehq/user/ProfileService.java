package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.org.DepartmentRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileView getProfile(JwtPrincipal principal) {
        User user = findSelf(principal);
        return toView(user);
    }

    @Transactional
    public ProfileView updateProfile(JwtPrincipal principal, UpdateProfileRequest request) {
        User user = findSelf(principal);
        user.setPhone(request.phone());
        user.setDateOfBirth(request.dateOfBirth());
        user.setBloodGroup(request.bloodGroup());
        user.setEmergencyContactName(request.emergencyContactName());
        user.setEmergencyContactRelationship(request.emergencyContactRelationship());
        user.setEmergencyContactPhone(request.emergencyContactPhone());
        userRepository.save(user);
        return toView(user);
    }

    @Transactional
    public void changePassword(JwtPrincipal principal, ChangePasswordRequest request) {
        User user = findSelf(principal);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findSelf(JwtPrincipal principal) {
        return userRepository.findById(principal.userId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ProfileView toView(User user) {
        String departmentName = user.getDepartmentId() != null
                ? departmentRepository.findById(user.getDepartmentId()).map(d -> d.getName()).orElse(null)
                : null;
        String managerName = user.getManagerId() != null
                ? userRepository.findById(user.getManagerId()).map(User::getFullName).orElse(null)
                : null;

        return new ProfileView(
                user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
                user.getPhone(), user.getDateOfBirth(), user.getBloodGroup(),
                user.getEmployeeCode(), user.getJobTitle(), departmentName, managerName, user.getHireDate(),
                user.getAvatarInitials(),
                user.getEmergencyContactName(), user.getEmergencyContactRelationship(), user.getEmergencyContactPhone());
    }
}
