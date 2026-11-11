package com.urua.urua_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String fullName;

    @Indexed(unique = true)
    private String email;

    private String phone;

    private String password;

    private Role role;

    // Sellers must be verified before they can list products (this is our trust feature)
    private boolean verified;

    // True once the user has clicked the link in their verification email
    private boolean emailVerified;

    // Only a hash of the emailed token is stored, like a password
    @Indexed
    private String verificationTokenHash;

    private Instant verificationExpiresAt;

    private Instant verificationSentAt;

    @Builder.Default
    private Instant createdAt = Instant.now();
}