package com.nforceone.nforcehq.user;

import com.nforceone.nforcehq.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-base-url:http://localhost:5174}")
    private String frontendBaseUrl;

    /** Always succeeds from the caller's perspective, whether or not the email exists — avoids leaking which emails have accounts. */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = generateRawToken();

            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hashToken(rawToken));
            token.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(token);

            String resetLink = frontendBaseUrl + "/#/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Invalid or expired reset link"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
