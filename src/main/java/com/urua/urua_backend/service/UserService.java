package com.urua.urua_backend.service;

import com.urua.urua_backend.dto.UserResponse;
import com.urua.urua_backend.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public UserResponse getProfile(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .verified(user.isVerified())
                .build();
    }
}