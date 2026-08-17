package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.common.MessageResponse;
import com.nforceone.nforcehq.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileView getProfile(@AuthenticationPrincipal JwtPrincipal principal) {
        return profileService.getProfile(principal);
    }

    @PatchMapping
    public ProfileView updateProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(principal, request);
    }

    @PostMapping("/change-password")
    public MessageResponse changePassword(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(principal, request);
        return new MessageResponse("Your password has been updated.");
    }

    @PostMapping("/photo")
    public ProfileView uploadPhoto(@AuthenticationPrincipal JwtPrincipal principal, @RequestParam("file") MultipartFile file) {
        return profileService.uploadPhoto(principal, file);
    }

    @DeleteMapping("/photo")
    public ProfileView deletePhoto(@AuthenticationPrincipal JwtPrincipal principal) {
        return profileService.deletePhoto(principal);
    }
}
