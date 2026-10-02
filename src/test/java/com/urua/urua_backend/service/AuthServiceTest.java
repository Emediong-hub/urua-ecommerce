package com.urua.urua_backend.service;

import com.urua.urua_backend.dto.AuthResponse;
import com.urua.urua_backend.dto.LoginRequest;
import com.urua.urua_backend.dto.RegisterRequest;
import com.urua.urua_backend.dto.UserResponse;
import com.urua.urua_backend.model.Role;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest(Role role) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Test Buyer");
        request.setEmail("Buyer@Test.com");
        request.setPhone("08012345678");
        request.setPassword("password123");
        request.setRole(role);
        return request;
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail("buyer@test.com");
        request.setPassword(password);
        return request;
    }

    private User savedUser() {
        return User.builder()
                .id("u1")
                .fullName("Test Buyer")
                .email("buyer@test.com")
                .password("hashed")
                .role(Role.BUYER)
                .build();
    }

    @Test
    void register_savesUserWithHashedPassword_andLowercaseEmail() {
        when(userRepository.existsByEmail("buyer@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("u1");
            return user;
        });

        UserResponse response = authService.register(registerRequest(Role.BUYER));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        assertEquals("hashed", captor.getValue().getPassword());
        assertEquals("buyer@test.com", response.getEmail());
        assertEquals(Role.BUYER, response.getRole());
        assertFalse(response.isVerified());
    }

    @Test
    void register_duplicateEmail_isRejectedWith409() {
        when(userRepository.existsByEmail("buyer@test.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(registerRequest(Role.BUYER)));

        assertEquals(409, ex.getStatusCode().value());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_adminRole_isRejectedWith400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.register(registerRequest(Role.ADMIN)));

        assertEquals(400, ex.getStatusCode().value());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_wrongPassword_isRejectedWith401() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(savedUser()));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginRequest("wrong")));

        assertEquals(401, ex.getStatusCode().value());
        verify(jwtService, never()).generateToken(any(User.class));
    }

    @Test
    void login_unknownEmail_isRejectedWith401() {
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(loginRequest("password123")));

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void login_correctPassword_returnsATokenAndTheUser() {
        User user = savedUser();
        when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token123");

        AuthResponse response = authService.login(loginRequest("password123"));

        assertEquals("token123", response.getToken());
        assertEquals("buyer@test.com", response.getUser().getEmail());
    }
}