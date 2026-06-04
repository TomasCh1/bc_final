package com.basketball.app.controller;

import com.basketball.app.dto.ForgotPasswordRequest;
import com.basketball.app.dto.ResetPasswordRequest;
import com.basketball.app.service.AuthService;
import com.basketball.app.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String FORGOT_PASSWORD_SUCCESS_MESSAGE =
            "Ak účet s týmto e-mailom existuje, poslali sme inštrukcie na obnovenie hesla.";

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Map<String, Object> response = authService.authenticate(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid credentials",
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {

        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", FORGOT_PASSWORD_SUCCESS_MESSAGE));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(
                    request.getToken(),
                    request.getNewPassword(),
                    request.getConfirmPassword());
            return ResponseEntity.ok(Map.of("message", "Heslo bolo úspešne zmenené. Môžete sa prihlásiť."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Reset failed",
                    "message", e.getMessage() != null ? e.getMessage() : "Neplatný alebo expirovaný odkaz."
            ));
        }
    }

    // DTOs
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}

