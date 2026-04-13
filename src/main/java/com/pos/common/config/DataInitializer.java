package com.pos.common.config;

import com.pos.auth.entity.Role;
import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.branch.entity.Branch;
import com.pos.branch.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() == 0) {

            // Seed a default branch for RECEPTION/CALL_CENTER demo users
            Branch defaultBranch = branchRepository.save(
                    Branch.builder().branchName("Main Branch").build()
            );

            createUser("admin",         "admin123",         "System Admin",      Role.ADMIN,         null);
            createUser("cashier",       "cashier123",       "Default Cashier",   Role.CASHIER,       null);
            createUser("adminbranches", "adminbranches123", "Branch Manager",    Role.ADMIN_BRANCHES, null);
            createUser("reception",     "reception123",     "Reception Desk",    Role.RECEPTION,     defaultBranch);
            createUser("callcenter",    "callcenter123",    "Call Center",       Role.CALL_CENTER,   defaultBranch);

            log.info("=================================================");
            log.info("  Default users seeded:");
            log.info("    admin         / admin123");
            log.info("    cashier       / cashier123");
            log.info("    adminbranches / adminbranches123");
            log.info("    reception     / reception123");
            log.info("    callcenter    / callcenter123");
            log.info("  Default branch: Main Branch");
            log.warn("  SECURITY: Change ALL passwords before production!");
            log.info("=================================================");
        }
    }

    private void createUser(String username, String password, String fullName, Role role, Branch branch) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(role)
                .branch(branch)
                .active(true)
                .build();
        userRepository.save(user);
    }
}
