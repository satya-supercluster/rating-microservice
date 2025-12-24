package com.ecommerce.ratingmicroservice.controller;

import com.ecommerce.ratingmicroservice.dto.request.ResendVerificationRequest;
import com.ecommerce.ratingmicroservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final UserService userService;

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        try {
            userService.verifyEmail(token);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("""
                <html>
                <body style="text-align:center; margin-top:50px; font-family:Arial">
                    <h2 style="color:green">✅ Email Verified!</h2>
                    <p>Your email has been successfully verified.</p>
                </body>
                </html>
                """);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<h2 style='color:red'>❌ Verification Failed</h2><p>" + e.getMessage() + "</p>");
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        userService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok("Verification email resent.");
    }
}