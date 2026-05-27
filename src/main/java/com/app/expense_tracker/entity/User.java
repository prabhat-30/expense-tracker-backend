package com.app.expense_tracker.entity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phoneNo;

    @JsonIgnore
    private String password; // Nullable to natively support Google/Apple SSO bypass bounds

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean isActive = false; // Set default false until validated via token/OTP link channels

    private String provider; // "LOCAL", "GOOGLE", "APPLE"
    private String providerId; // Unique asset tracking tag returned from OAuth providers

    private String otp;
    private LocalDateTime otpExpiry;

    @Lob
    @Column(name = "avatar_image", columnDefinition = "TEXT")
    private String avatarImage;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(){
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
