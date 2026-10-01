package com.zidio.keystone.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sends the password-reset email from a dummy/project-only mailbox (never a personal one).
 *
 * Two transports:
 *  1. HTTPS mail API (Brevo) - used when BREVO_API_KEY is set. Needed on hosts that block
 *     outbound SMTP ports (Render's free tier blocks 25/465/587), because HTTPS (443) is never blocked.
 *  2. SMTP via JavaMailSender (the approach taught in class) - used otherwise, e.g. locally
 *     with MailHog or a Gmail app password.
 *
 * Sending runs in the background so the API answers immediately, and a failure is only logged:
 * forgot-password must respond the same way either way, so we never reveal whether an
 * email address exists in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final JavaMailSender mailSender;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Reset your Keystone password";
        String text = "We received a request to reset your Keystone password.\n\n" +
                "Click the link below to choose a new password. This link expires in 30 minutes:\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email.";

        // Run in the background so the user is not kept waiting on the mail server.
        CompletableFuture.runAsync(() -> {
            try {
                if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                    sendViaBrevo(toEmail, subject, text);
                } else {
                    sendViaSmtp(toEmail, subject, text);
                }
            } catch (Exception e) {
                log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            }
        });
    }

    private void sendViaSmtp(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        log.info("Password reset email sent via SMTP to {}", toEmail);
    }

    private void sendViaBrevo(String toEmail, String subject, String text) throws Exception {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", "Keystone", "email", fromAddress),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", text
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_URL))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 == 2) {
            log.info("Password reset email sent via Brevo to {}", toEmail);
        } else {
            log.error("Brevo rejected the email (HTTP {}): {}", response.statusCode(), response.body());
        }
    }
}
