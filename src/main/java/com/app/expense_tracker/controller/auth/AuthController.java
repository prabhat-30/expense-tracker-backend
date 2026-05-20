package com.app.expense_tracker.controller.auth;

import com.app.expense_tracker.dto.*;
import com.app.expense_tracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service){
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(service.register(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam("token") String token) {
        try {
            // 🌟 FIX: Cleanly decode any corrupted browser characters back to original text
            String decodedToken = URLDecoder.decode(token, StandardCharsets.UTF_8.name());

            // Pass the safely cleaned token into your verification token repository logic
            String result = service.verifyEmailToken(decodedToken);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(service.login(request));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@RequestBody Map<String, String> payload) {
        String identity = payload.get("identity");
        String outputMessage = service.dispatchLoginOtp(identity);
        return ResponseEntity.ok(Map.of("message", outputMessage));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> loginViaOtp(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.verifyOtpAndLogin(request));
    }

    @PostMapping("/social-login")
    public ResponseEntity<AuthResponse> socialAuthentication(@Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(service.processSocialAuthentication(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String message = service.generatePasswordResetLink(email);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reset-password/save")
    public ResponseEntity<Map<String, String>> executePasswordReset(@Valid @RequestBody ResetPasswordRequest request) {
        String message = service.executePasswordReset(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PutMapping("/profile/change-password")
    public ResponseEntity<Map<String, String>> changeProfilePassword(
            @Valid @RequestBody ProfilePasswordChangeRequest request, Authentication authentication) {
        service.changeProfilePassword(authentication.getName(), request);
        return ResponseEntity.ok(Map.of("message", "Password successfully rotated."));
    }
}