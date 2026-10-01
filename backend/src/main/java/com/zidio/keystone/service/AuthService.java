package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.domain.Role;
import com.zidio.keystone.domain.User;
import com.zidio.keystone.dto.ForgotPasswordRequest;
import com.zidio.keystone.dto.LoginRequest;
import com.zidio.keystone.dto.LoginResponse;
import com.zidio.keystone.dto.MessageResponse;
import com.zidio.keystone.dto.RegisterRequest;
import com.zidio.keystone.dto.ResetPasswordRequest;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.UserRepository;
import com.zidio.keystone.security.JwtService;
import com.zidio.keystone.security.TokenKillService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long RESET_TOKEN_VALID_MINUTES = 30;

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final TokenKillService tokenKillService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName(), user.getRole().name());

        return new LoginResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.getCustomerId());
    }

    /**
     * Public self-registration. Deliberately the only path that creates a User without
     * going through UserService.create() (which requires Permission.CREATE_USER) -
     * this is the one intentional exception, and it always produces a CUSTOMER account
     * tied to a brand-new Customer org, never an internal role.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("An account with this email already exists: " + request.email());
        }

        String orgName = (request.companyName() == null || request.companyName().isBlank())
                ? request.name() + "'s Account"
                : request.companyName();

        Customer customer = customerRepository.save(Customer.builder()
                .name(orgName)
                .contactEmail(request.email())
                .build());

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .customerId(customer.getId())
                .active(true)
                .build());

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName(), user.getRole().name());

        return new LoginResponse(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), user.getCustomerId());
    }

    /**
     * Forgot password: always returns the same generic message whether or not the
     * email exists in our database - responding differently ("no account found" vs
     * "email sent") would let anyone probe which emails are registered. If the user
     * exists, we save a one-time token with a 30-minute expiry and email a reset link;
     * if not, we just do nothing but still return the same success message.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiresAt(Instant.now().plus(RESET_TOKEN_VALID_MINUTES, ChronoUnit.MINUTES));
            userRepository.save(user);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });

        return new MessageResponse("If an account exists for that email, a reset link has been sent.");
    }

    /**
     * Reset password: the token must exist and not be expired. Either failure gets the
     * same "invalid or expired" message, so an attacker can't tell expired tokens from
     * tokens that never existed. On success the token is cleared immediately so the
     * link can only ever be used once.
     */
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetToken(request.token())
                .filter(u -> u.getResetTokenExpiresAt() != null && u.getResetTokenExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has expired. Please request a new one."));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);

        return new MessageResponse("Your password has been reset. You can now log in with your new password.");
    }

    /**
     * Logout: pull the JWT out of the Authorization header (same substring(7) trick
     * as the filter) and add it to the blocklist so it can never be used again.
     */
    public MessageResponse logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            tokenKillService.blockListToken(authorizationHeader.substring(7));
        }
        return new MessageResponse("Logged out successfully");
    }
}
