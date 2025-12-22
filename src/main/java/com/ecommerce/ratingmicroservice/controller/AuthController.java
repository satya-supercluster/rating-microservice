package com.ecommerce.ratingmicroservice.controller;

import com.ecommerce.ratingmicroservice.dto.request.LoginRequest;
import com.ecommerce.ratingmicroservice.dto.request.RegisterRequest;
import com.ecommerce.ratingmicroservice.entity.User;
import com.ecommerce.ratingmicroservice.repository.UserRepository;
import com.ecommerce.ratingmicroservice.security.JwtUtil;
import com.ecommerce.ratingmicroservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok("User registered successfully. Please verify email.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + jwt)
                .body(Map.of("token", jwt));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Remove sensitive data
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}