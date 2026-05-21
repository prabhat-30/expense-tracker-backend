package com.app.expense_tracker.config;

import com.app.expense_tracker.entity.Role;
import com.app.expense_tracker.entity.User;
import com.app.expense_tracker.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        String adminUsername = "admin".toLowerCase();

        if (userRepository.findByUsername(adminUsername).isEmpty()) {

            User admin = new User();

            admin.setUsername(adminUsername);

            admin.setPassword(
                    passwordEncoder.encode("admin123")
            );

            admin.setRole(Role.ADMIN);
            admin.setEmail("admin@gmail.com");

            admin.setActive(true);

            userRepository.save(admin);

            System.out.println(
                    "ADMIN CREATED SUCCESSFULLY"
            );
        }
    }
}
