package com.urua.urua_backend.controller;

import com.urua.urua_backend.dto.AuthResponse;
import com.urua.urua_backend.dto.LoginRequest;
import com.urua.urua_backend.dto.MessageResponse;
import com.urua.urua_backend.dto.RegisterRequest;
import com.urua.urua_backend.dto.ResendVerificationRequest;
import com.urua.urua_backend.dto.UserResponse;
import com.urua.urua_backend.dto.VerifyEmailRequest;
import com.urua.urua_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verify-email")
    public MessageResponse verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.getToken());
        return new MessageResponse("Your email has been verified. You can now log in.");
    }

    @PostMapping("/resend-verification")
    public MessageResponse resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.getEmail());
        return new MessageResponse("If that account exists and still needs verifying, a new email is on its way.");
    }
}