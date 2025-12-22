package com.ecommerce.ratingmicroservice.mapper;

import com.ecommerce.ratingmicroservice.dto.request.RegisterRequest;
import com.ecommerce.ratingmicroservice.entity.User;

import java.time.LocalDateTime;
import java.util.Set;

public class UserMapperUtil {

    private UserMapperUtil() {}

    public static User toEntity(RegisterRequest request, String encodedPassword) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(encodedPassword);
        user.setRoles(
                request.getRoles() == null || request.getRoles().isEmpty()
                        ? Set.of("USER")
                        : request.getRoles()
        );
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(false);
        return user;
    }
}
