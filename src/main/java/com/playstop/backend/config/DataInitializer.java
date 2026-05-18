package com.playstop.backend.config;

import com.playstop.backend.entity.User;
import com.playstop.backend.enums.Role;
import com.playstop.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_EMAIL    = "admin@playstop.com";
    private static final String ADMIN_PASSWORD = "Admin@PlayStop2026";
    private static final String ADMIN_NAME     = "Administrador PlayStop";

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(ADMIN_EMAIL)) {
            User admin = User.builder()
                .name(ADMIN_NAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
            userRepository.save(admin);
            log.info("Admin user created: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
        }
    }
}
