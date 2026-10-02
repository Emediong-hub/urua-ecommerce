package com.urua.urua_backend.dto;

import com.urua.urua_backend.model.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private boolean verified;
}