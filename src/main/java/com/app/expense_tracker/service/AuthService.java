package com.app.expense_tracker.service;

import com.app.expense_tracker.dto.*;
import com.app.expense_tracker.entity.*;
import com.app.expense_tracker.repository.*;
import com.app.expense_tracker.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository tokenRepository,
                       VerificationTokenRepository verificationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditLogService auditLogService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
        this.emailService = emailService;
    }

    @Transactional
    public String register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Validation Error: Passwords do not match.");
        }

        String username = request.getUsername().toLowerCase();
        String email = request.getEmail().toLowerCase();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email address already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .username(username)
                .email(email)
                .phoneNo(request.getPhoneNo())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isActive(false) // Deactivated until email link verification is successful
                .provider("LOCAL")
                .build();

        userRepository.save(user);

        // Generate email verification token link
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(verificationToken);

        String activationLink = "http://localhost:5173/verify-email?token=" + token;
        emailService.sendSimpleMessage(user.getEmail(), "Activate Your Ledger Account",
                "Welcome to ExpenseTracker! Please click the following activation link to verify your account: " + activationLink);

        auditLogService.log(username, "REGISTER", "New user registered. Verification link dispatched.");
        return "Registration successful! Please check your email to verify and activate your account.";
    }

    @Transactional
    public String verifyEmailToken(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid activation link token."));

        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(vt);
            throw new RuntimeException("Activation link expired. Please sign up again.");
        }

        User user = vt.getUser();
        user.setActive(true); // Activate user account row cleanly
        userRepository.save(user);
        verificationTokenRepository.delete(vt);

        auditLogService.log(user.getUsername(), "EMAIL_VERIFIED", "Account successfully activated.");
        return "Account verified successfully! You can now log in.";
    }

    public AuthResponse login(LoginRequest request) {
        String identity = request.getIdentity().toLowerCase();

        // Dynamic search strategy: check username, email, or phone number fields
        User user = userRepository.findByUsername(identity)
                .or(() -> userRepository.findByEmail(identity))
                .or(() -> userRepository.streamAllByPhoneNo(identity)) // Added custom stream fallback
                .orElseThrow(() -> new RuntimeException("Invalid account details or credentials."));

        if (!user.isActive()) {
            throw new RuntimeException("Your account has not been activated yet. Please verify your email link.");
        }

        // Validate password credentials match
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            auditLogService.log(user.getUsername(), "LOGIN_FAILED", "Invalid password entry attempted.");
            throw new RuntimeException("Invalid username or password");
        }

        auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", "User logged in successfully via local credentials.");
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name());
    }

    @Transactional
    public String dispatchLoginOtp(String emailOrPhone) {
        String lookupTarget = emailOrPhone.toLowerCase().trim();
        User user = userRepository.findByEmail(lookupTarget)
                .or(() -> userRepository.streamAllByPhoneNo(lookupTarget))
                .orElseThrow(() -> new RuntimeException("No active account matching this identifier was found."));

        // OTP Cooldown Throttling Defense
        if (user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now().plusMinutes(4))) {
            throw new RuntimeException("Rate Limit: Please wait 1 minute before requesting another verification OTP.");
        }

        // Generate dynamic 6-digit numeric OTP badge
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // 🌟 FIXED DYNAMIC ROUTING ENGINE
        if (lookupTarget.matches("\\d+")) {
            // Condition triggered: The user logged in via an all-numeric mobile number phone string!
            System.out.println("📱 SMS OTP SYSTEM TRIGGERED FOR: " + user.getPhoneNo());
            System.out.println("OTP Verification Code: [" + otp + "]");

            // TODO: Once you subscribe to an SMS provider (Twilio/Infobip), link your handler here:
            // smsService.sendMessage(user.getPhoneNo(), "Your OTP verification code is: " + otp);

            // For local development, we fallback to printing to console and letting it fall back to email if needed
            emailService.sendSimpleMessage(user.getEmail(), "Your Secure Login OTP (Mobile Fallback)",
                    "Your temporary mobile login verification code is: " + otp);
        } else {
            // Condition triggered: The user typed an alpha-numeric email string structure
            emailService.sendSimpleMessage(user.getEmail(), "Your Secure Login OTP",
                    "Your temporary ExpenseTracker login verification code is: " + otp + "\nThis code will expire in 5 minutes.");
        }

        auditLogService.log(user.getUsername(), "OTP_DISPATCHED", "Login OTP dispatched to verified node.");
        return "A verification OTP has been sent successfully.";
    }

    @Transactional
    public AuthResponse verifyOtpAndLogin(LoginRequest request) {
        String identity = request.getIdentity().toLowerCase();
        User user = userRepository.findByEmail(identity)
                .or(() -> userRepository.streamAllByPhoneNo(identity))
                .orElseThrow(() -> new RuntimeException("Invalid login verification credentials."));

        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid verification OTP code. Please try again.");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("The verification OTP code has expired. Please request a new one.");
        }

        // Single-use protection guard: Burn token immediately upon match execution
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", "User logged in successfully via transaction OTP path.");
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name());
    }

    @Transactional
    public AuthResponse processSocialAuthentication(SocialLoginRequest request) {
        String email = request.getEmail().toLowerCase();

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // Auto-registration on matching social footprint miss signatures
            String generatedUsername = email.split("@")[0] + "_" + request.getProvider().toLowerCase();
            User newUser = User.builder()
                    .name(request.getName())
                    .username(generatedUsername)
                    .email(email)
                    .role(Role.USER)
                    .isActive(true) // Socially verified accounts bypass manual links
                    .provider(request.getProvider())
                    .providerId(request.getProviderId())
                    .build();
            return userRepository.save(newUser);
        });

        auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", "Logged in via social identity provider: " + request.getProvider());
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getRole().name());
    }

    @Transactional
    public String generatePasswordResetLink(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("No account registered with this email address."));

        tokenRepository.deleteByUser(user); // Invalidate any old floating tokens

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(15)); // Strict 15-minute expiration rule
        tokenRepository.save(resetToken);

        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        emailService.sendSimpleMessage(user.getEmail(), "Secure Password Reset Link Request",
                "We received a request to reset your credentials. Click the link below to configure your new password:\n" + resetLink + "\n\nThis link will automatically expire in 15 minutes.");

        return "Password reset link successfully generated and dispatched to your Gmail account.";
    }

    // =========================================================================
    // PRODUCTION SECURE SYSTEM: EXECUTES AND CONSUMES THE OVERLAY LINK TOKEN
    // =========================================================================
    @Transactional
    public String executePasswordReset(ResetPasswordRequest request) {
        // 1. Locate the dynamic token string record from its matching table mapping layer
        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or corrupted token registry link. This recovery token has already been used or is invalid."));

        // 2. Expiration Check: Verify token has not crossed its strict 15-minute deadline
        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new RuntimeException("This password reset link has expired. Please request a new link.");
        }

        // 3. User Validation: Ensure the typed passwords fields match each other perfectly
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match.");
        }

        // 4. Retrieve user associated with this specific reset token node row
        User user = resetToken.getUser();

        // 5. 🌟 BUG FIX: Security Check to verify the new password is NOT identical to the old password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Security Mismatch: Your new password cannot be identical to your current password. Please choose a different one.");
        }

        // 6. Encrypt and apply the newly validated password hash sequence parameters securely
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 7. 🌟 SINGLE USE PROTECTION: Burn token record immediately out of DB to prevent reuse replay attacks
        tokenRepository.delete(resetToken);

        // 8. Track successful security action to internal audit log tables
        auditLogService.log(user.getUsername(), "PASSWORD_RESET_SUCCESS", "Password updated via forgot-password email link.");

        return "Password successfully reset! Redirecting to login...";
    }

    @Transactional
    public void changeProfilePassword(String username, ProfilePasswordChangeRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User session profile context not found."));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("The old password you entered is incorrect.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirmation fields do not match.");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("New password cannot be identical to your old password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditLogService.log(username, "PASSWORD_ROTATED_PROFILE", "User updated account password inside profile console.");
    }
}
