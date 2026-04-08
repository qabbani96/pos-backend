package com.pos.auth.service;

import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.dto.UserResponse;
import com.pos.auth.entity.Role;
import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    /**
     * Roles that an ADMIN is allowed to create via the API.
     * ADMIN and CASHIER are excluded — they are system roles managed separately.
     */
    private static final Set<Role> CREATABLE_ROLES = Set.of(Role.RECEPTION, Role.CALL_CENTER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user with a restricted role (RECEPTION or CALL_CENTER).
     * Only ADMIN can call this; the controller enforces that via @PreAuthorize.
     */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // 1. Validate role is one of the allowed values
        Role role = parseAndValidateRole(request.role());

        // 2. Check username uniqueness
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("Username already exists: " + request.username());
        }

        // 3. Build and persist
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(role)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created: username={}, role={}", saved.getUsername(), saved.getRole());

        return UserResponse.from(saved);
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Role parseAndValidateRole(String roleValue) {
        Role role;
        try {
            role = Role.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Invalid role '" + roleValue + "'. Allowed values: RECEPTION, CALL_CENTER"
            );
        }

        if (!CREATABLE_ROLES.contains(role)) {
            throw new BusinessException(
                    "Role '" + roleValue + "' cannot be assigned via this API. Allowed values: RECEPTION, CALL_CENTER"
            );
        }

        return role;
    }
}
