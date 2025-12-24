package com.ecommerce.ratingmicroservice.service;

import com.ecommerce.ratingmicroservice.dto.request.RegisterRequest;
import com.ecommerce.ratingmicroservice.entity.User;
import com.ecommerce.ratingmicroservice.repository.UserRepository;
import com.ecommerce.ratingmicroservice.security.EmailVerificationTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailVerificationTokenUtil tokenUtil;

    public void register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        Set<String> roles = request.getRoles() != null && !request.getRoles().isEmpty()
                ? request.getRoles()
                : Set.of("USER");

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .isEmailVerified(false)
                .build();
        User savedUser = userRepository.save(user);

        try {
            String token = tokenUtil.generateToken(savedUser.getEmail());
            emailService.sendVerificationEmail(savedUser.getEmail(), token);
        } catch (Exception e) {
            userRepository.delete(savedUser);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        if (!tokenUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or expired verification token");
        }

        String email = tokenUtil.extractEmail(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email already verified");
        }

        String token = tokenUtil.generateToken(email);
        try {
            emailService.sendVerificationEmail(email, token);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resend verification email", e);
        }
    }
}