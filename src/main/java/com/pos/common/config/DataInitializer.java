package com.pos.common.config;

import com.pos.auth.entity.Role;
import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Admin")
                    .role(Role.ADMIN)
                    .active(true)
                    .build();

            userRepository.save(admin);

            User casher = User.builder()
                    .username("casher")
                    .password(passwordEncoder.encode("casher123"))
                    .fullName("System Admin")
                    .role(Role.CASHIER)
                    .active(true)
                    .build();
            userRepository.save(casher);

            log.info("=================================================");
            log.info("  Default admin user created.");
            log.info("  Username : admin");
            log.info("  Password : admin123");
            log.warn("  SECURITY : Change this password immediately!");
            log.info("=================================================");
        }
    }
}
