package com.urua.urua_backend.service;

import com.urua.urua_backend.model.Role;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final String FRONTEND = "http://localhost:5173";
    private static final String LINK_START = FRONTEND + "/verify-email?token=";

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(userRepository, emailService, FRONTEND);
    }

    private User newUser() {
        return User.builder()
                .id("u1")
                .fullName("Test Buyer")
                .email("buyer@test.com")
                .role(Role.BUYER)
                .emailVerified(false)
                .build();
    }

    // Sends a verification and returns the token that was placed in the emailed link
    private String sendAndCaptureToken(User user) {
        service.sendVerification(user);

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq("buyer@test.com"), eq("Test Buyer"), link.capture());

        return link.getValue().substring(LINK_START.length());
    }

    @Test
    void sendVerification_storesOnlyAHashOfTheToken_andEmailsTheLink() {
        User user = newUser();

        String token = sendAndCaptureToken(user);

        assertNotNull(user.getVerificationTokenHash());
        assertNotEquals(token, user.getVerificationTokenHash());
        assertTrue(user.getVerificationExpiresAt().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));
        verify(userRepository).save(user);
    }

    @Test
    void verify_validToken_marksTheEmailVerified_andClearsTheToken() {
        User user = newUser();
        String token = sendAndCaptureToken(user);
        when(userRepository.findByVerificationTokenHash(user.getVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        service.verify(token);

        assertTrue(user.isEmailVerified());
        assertNull(user.getVerificationTokenHash());
        assertNull(user.getVerificationExpiresAt());
    }

    @Test
    void verify_expiredToken_isRejectedWith400() {
        User user = newUser();
        String token = sendAndCaptureToken(user);
        user.setVerificationExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findByVerificationTokenHash(user.getVerificationTokenHash()))
                .thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verify(token));

        assertEquals(400, ex.getStatusCode().value());
        assertFalse(user.isEmailVerified());
    }

    @Test
    void verify_unknownToken_isRejectedWith400() {
        when(userRepository.findByVerificationTokenHash(anyString())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verify("not-a-real-token"));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void resend_withinSixtySeconds_doesNotSendAnotherEmail() {
        User user = newUser();
        user.setVerificationSentAt(Instant.now());
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        service.resend("Buyer@Test.com");

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void resend_afterTheWait_sendsANewEmail() {
        User user = newUser();
        user.setVerificationSentAt(Instant.now().minus(2, ChronoUnit.MINUTES));
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        service.resend("buyer@test.com");

        verify(emailService).sendVerificationEmail(eq("buyer@test.com"), eq("Test Buyer"), anyString());
    }

    @Test
    void resend_forAnAlreadyVerifiedUser_sendsNothing() {
        User user = newUser();
        user.setEmailVerified(true);
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));

        service.resend("buyer@test.com");

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendVerification_whenTheEmailFails_stillDoesNotThrow() {
        doThrow(new IllegalStateException("smtp down"))
                .when(emailService).sendVerificationEmail(anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> service.sendVerification(newUser()));
    }
}