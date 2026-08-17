package com.nforceone.nforcehq.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over Resend's REST API — deliberately not a full SDK dependency,
 * just one POST call. When resend.api-key isn't configured (local dev without a
 * key), the reset link is logged instead of sent, so the flow still works end to
 * end without requiring every developer to have a Resend account.
 */
@Slf4j
@Service
public class EmailService {

    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String fromEmail;

    public EmailService(
            @Value("${resend.api-key:}") String apiKey,
            @Value("${resend.from-email:onboarding@resend.dev}") String fromEmail) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Reset your nForceHQ password";
        String html = "<p>We received a request to reset your nForceHQ password.</p>"
                + "<p><a href=\"" + resetLink + "\">Click here to choose a new password</a>. "
                + "This link expires in 30 minutes.</p>"
                + "<p>If you didn't request this, you can safely ignore this email.</p>";

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("resend.api-key not configured — password reset link for {} (not emailed): {}", toEmail, resetLink);
            return;
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "from", fromEmail,
                    "to", List.of(toEmail),
                    "subject", subject,
                    "html", html));

            HttpRequest request = HttpRequest.newBuilder(RESEND_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.error("Resend API returned {} sending reset email to {}: {}", response.statusCode(), toEmail, response.body());
            }
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}", toEmail, ex);
        }
    }
}
