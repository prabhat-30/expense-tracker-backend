package com.app.expense_tracker.controller;

import com.app.expense_tracker.entity.User;
import com.app.expense_tracker.repository.UserRepository;
import com.app.expense_tracker.service.AuditLogService; // 🌟 FIXED: Imported your AuditLogService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService; // 🌟 FIXED: Injected the audit logger instance

    // 1. ENDPOINT TO FETCH PROFILE AVATAR ON LOGIN
    @GetMapping("/profile/avatar")
    public ResponseEntity<?> getAvatar(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User session not found"));
        return ResponseEntity.ok(Map.of("avatar", user.getAvatarImage() != null ? user.getAvatarImage() : ""));
    }

    // 2. ENDPOINT TO UPLOAD / SAVE PROFILE AVATAR
    @PutMapping("/profile/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestBody Map<String, String> payload, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User session not found"));

        String base64Image = payload.get("avatar");
        user.setAvatarImage(base64Image);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile picture synchronized to account successfully"));
    }

    // 3. ENDPOINT: Pulls full account identity directly from user table securely
    @GetMapping("/profile/details")
    public ResponseEntity<?> getProfileDetails(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User session not found"));

        return ResponseEntity.ok(Map.of(
                "name", user.getName() != null ? user.getName() : "User Account",
                "username", user.getUsername(),
                "email", user.getEmail(),
                "phoneNo", user.getPhoneNo() != null ? user.getPhoneNo() : "Not Linked"
        ));
    }

    // 4. REINFORCED PROFILE SAVE ENDPOINT WITH SECURITY AUDIT TRACKING PIPELINE
    @PutMapping("/profile/update-identity")
    public ResponseEntity<?> updateProfileIdentity(
            @RequestBody Map<String, String> payload, Authentication authentication) {

        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User session profile context not found."));

        String providedPassword = payload.get("password");
        String newName = payload.get("name");
        String newUsername = payload.get("username").toLowerCase().trim();
        String newEmail = payload.get("email").toLowerCase().trim();
        String newPhone = payload.get("phoneNo").trim();

        // Security Passcode Validation Check
        if (providedPassword == null || !passwordEncoder.matches(providedPassword, currentUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Security Verification Failed: Incorrect password entered."));
        }

        // Unique Constraint Check: Username
        if (!newUsername.equalsIgnoreCase(currentUser.getUsername()) && userRepository.findByUsername(newUsername).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Conflict Error: This username is already taken. Please choose a unique one."));
        }

        // Unique Constraint Check: Email
        if (!newEmail.equalsIgnoreCase(currentUser.getEmail()) && userRepository.findByEmail(newEmail).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Conflict Error: This email address is already registered to another account."));
        }

        // Unique Constraint Check: Phone Number
        if (!newPhone.isEmpty() && !newPhone.equalsIgnoreCase(currentUser.getPhoneNo()) && userRepository.streamAllByPhoneNo(newPhone).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Conflict Error: This phone number is already linked to another account."));
        }

        // 🌟 BUILD AUDIT RECORD: Collect details of what is about to change before writing to DB
        StringBuilder logDetails = new StringBuilder("User modified account metadata parameters: ");
        if (!newName.equals(currentUser.getName())) {
            logDetails.append(String.format("[Name: '%s' -> '%s'] ", currentUser.getName(), newName));
        }
        if (!newUsername.equals(currentUser.getUsername())) {
            logDetails.append(String.format("[Username: '%s' -> '%s'] ", currentUser.getUsername(), newUsername));
        }
        if (!newEmail.equals(currentUser.getEmail())) {
            // Secure Compliance: Mask sensitive email data strings in log table view
            String maskedEmail = newEmail.replaceAll("(?<=.).(?=[^@]*?@)", "*");
            logDetails.append(String.format("[Email changed to: %s] ", maskedEmail));
        }
        if (!newPhone.equals(currentUser.getPhoneNo() != null ? currentUser.getPhoneNo() : "")) {
            String maskedPhone = newPhone.isEmpty() ? "Cleared" : newPhone.replaceAll(".(?=.{3})", "*");
            logDetails.append(String.format("[Phone changed to: %s] ", maskedPhone));
        }

        // Commit modifications to database permanently
        currentUser.setName(newName);
        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        currentUser.setPhoneNo(newPhone.isEmpty() ? null : newPhone);
        userRepository.save(currentUser);

        // 🌟 FIXED: Broadcast action details straight into your Security logs registry pipeline!
        auditLogService.log(
                currentUser.getUsername(),
                "UPDATE_PROFILE_INFO",
                logDetails.toString()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Profile identity saved to database cleanly.",
                "name", currentUser.getName(),
                "username", currentUser.getUsername(),
                "email", currentUser.getEmail(),
                "phoneNo", currentUser.getPhoneNo() != null ? currentUser.getPhoneNo() : "Not Linked"
        ));
    }
}