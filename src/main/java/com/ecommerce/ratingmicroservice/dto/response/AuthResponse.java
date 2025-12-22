package com.ecommerce.ratingmicroservice.dto.response;


import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private UserResponse user;

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }
}