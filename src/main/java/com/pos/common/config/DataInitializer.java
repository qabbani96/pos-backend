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
            createUser("admin",       "admin123",       "System Admin",    Role.ADMIN);
            createUser("cashier",     "cashier123",     "Default Cashier", Role.CASHIER);
            createUser("reception",   "reception123",   "Reception Desk",  Role.RECEPTION);
            createUser("callcenter",  "callcenter123",  "Call Center",     Role.CALL_CENTER);

            log.info("=================================================");
            log.info("  Default users seeded:");
            log.info("    admin       / admin123");
            log.info("    cashier     / cashier123");
            log.info("    reception   / reception123");
            log.info("    callcenter  / callcenter123");
            log.warn("  SECURITY: Change ALL passwords before production!");
            log.info("=================================================");
        }
    }

    private void createUser(String username, String password, String fullName, Role role) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(role)
                .active(true)
                .build();
        userRepository.save(user);
    }
}
