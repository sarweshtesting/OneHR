package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.common.ApiException;
import com.nforceone.nforcehq.org.DepartmentRepository;
import com.nforceone.nforcehq.security.JwtPrincipal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private static final int MAX_PHOTO_BYTES = 2 * 1024 * 1024;

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

    @Transactional
    public ProfileView uploadPhoto(JwtPrincipal principal, MultipartFile file) {
        User user = findSelf(principal);
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo file is empty");
        }
        if (!ALLOWED_PHOTO_TYPES.contains(file.getContentType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported image type: " + file.getContentType());
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Photo must be 2MB or smaller");
        }
        try {
            user.setAvatarPhoto(file.getBytes());
            user.setAvatarContentType(file.getContentType());
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read uploaded photo");
        }
        userRepository.save(user);
        return toView(user);
    }

    @Transactional
    public ProfileView deletePhoto(JwtPrincipal principal) {
        User user = findSelf(principal);
        user.setAvatarPhoto(null);
        user.setAvatarContentType(null);
        userRepository.save(user);
        return toView(user);
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
                user.getAvatarInitials(), AvatarUtil.dataUri(user),
                user.getEmergencyContactName(), user.getEmergencyContactRelationship(), user.getEmergencyContactPhone());
    }
}
