package com.zidio.keystone.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * This is the "mail sender" piece from class - a dummy/project-only mailbox (never
 * your personal one) sends the reset link. In local dev, point spring.mail.host at a
 * fake SMTP catcher like MailHog/Mailtrap (default localhost:1025) so you can read the
 * email without actually sending it anywhere; set real SMTP credentials (e.g. a Gmail
 * app password on your dummy account) only when you deploy.
 *
 * If sending fails (bad credentials, SMTP unreachable), we log it rather than blow up
 * the request - forgot-password should still respond normally either way, so we never
 * reveal to a caller whether an email address exists in our database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Reset your Keystone password");
            message.setText(
                    "We received a request to reset your Keystone password.\n\n" +
                    "Click the link below to choose a new password. This link expires in 30 minutes:\n" +
                    resetLink + "\n\n" +
                    "If you didn't request this, you can safely ignore this email."
            );
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
