package com.ecommerce.ratingmicroservice.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private boolean isEmailVerified;
}