package com.urua.urua_backend.service;

import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private static final Duration RESEND_GAP = Duration.ofSeconds(60);

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public EmailVerificationService(UserRepository userRepository,
                                    EmailService emailService,
                                    @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    // Creates a fresh one-time token, stores only its hash, and emails the link
    public void sendVerification(User user) {
        String token = newToken();

        user.setVerificationTokenHash(hash(token));
        user.setVerificationExpiresAt(Instant.now().plus(TOKEN_LIFETIME));
        user.setVerificationSentAt(Instant.now());
        userRepository.save(user);

        String link = frontendUrl + "/verify-email?token=" + token;

        try {
            emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), link);
        } catch (RuntimeException e) {
            // Registration still succeeds. The user can request another email.
            Throwable root = e;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            log.warn("Could not send verification email to {}: {}", user.getEmail(), root.getMessage());
        }
    }

    public void verify(String token) {
        User user = userRepository.findByVerificationTokenHash(hash(token.trim()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "This link is invalid or has already been used."));

        if (user.getVerificationExpiresAt() == null
                || user.getVerificationExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This link has expired. Log in to request a new one.");
        }

        user.setEmailVerified(true);
        // The token works only once
        user.setVerificationTokenHash(null);
        user.setVerificationExpiresAt(null);
        userRepository.save(user);
    }

    // Always behaves the same, so nobody can use this to find out which emails are registered
    public void resend(String email) {
        userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> !user.isEmailVerified())
                .filter(user -> user.getVerificationSentAt() == null
                        || user.getVerificationSentAt().plus(RESEND_GAP).isBefore(Instant.now()))
                .ifPresent(this::sendVerification);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}