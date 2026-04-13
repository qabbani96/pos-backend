package com.pos.auth.service;

import com.pos.auth.dto.CreateUserRequest;
import com.pos.auth.dto.UpdateUserRequest;
import com.pos.auth.dto.UserResponse;
import com.pos.auth.entity.Role;
import com.pos.auth.entity.User;
import com.pos.auth.repository.UserRepository;
import com.pos.branch.entity.Branch;
import com.pos.branch.service.BranchService;
import com.pos.common.exception.BusinessException;
import com.pos.common.exception.ResourceNotFoundException;
import com.pos.shop.entity.Shop;
import com.pos.shop.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    /**
     * Roles that ADMIN / ADMIN_BRANCHES can create or assign via the API.
     */
    private static final Set<Role> MANAGEABLE_ROLES =
            Set.of(Role.RECEPTION, Role.CALL_CENTER, Role.CASHIER, Role.INVENTORY);

    /** Roles that require a branch to be assigned. */
    private static final Set<Role> BRANCH_REQUIRED_ROLES = Set.of(Role.RECEPTION, Role.CALL_CENTER);

    /** Roles that require a shop to be assigned. */
    private static final Set<Role> SHOP_REQUIRED_ROLES = Set.of(Role.CASHIER, Role.INVENTORY);

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchService   branchService;
    private final ShopService     shopService;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        Role role = parseAndValidateRole(request.role());

        if (callerIsAdminBranchesOnly() && role == Role.CASHIER) {
            throw new BusinessException(
                    "ROLE_NOT_ALLOWED",
                    "ADMIN_BRANCHES cannot create users with the CASHIER role."
            );
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("USERNAME_TAKEN", "Username already exists: " + request.username());
        }

        Branch branch = resolveBranch(role, request.branchId());
        Shop   shop   = resolveShop(role, request.shopId());

        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(role)
                .branch(branch)
                .shop(shop)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User created: username={}, role={}, shop={}", saved.getUsername(), saved.getRole(),
                shop != null ? shop.getName() : "none");
        return UserResponse.from(saved);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findOrThrow(id);

        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            Role newRole = parseAndValidateRole(request.role());
            if (callerIsAdminBranchesOnly() && newRole == Role.CASHIER) {
                throw new BusinessException(
                        "ROLE_NOT_ALLOWED",
                        "ADMIN_BRANCHES cannot assign the CASHIER role."
                );
            }
            user.setRole(newRole);
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        if (request.branchId() != null) {
            user.setBranch(request.branchId() == -1L ? null : branchService.findOrThrow(request.branchId()));
        }
        if (request.shopId() != null) {
            user.setShop(request.shopId() == -1L ? null : shopService.getOrThrow(request.shopId()));
        }

        User saved = userRepository.save(user);
        log.info("User updated: id={}, username={}", saved.getId(), saved.getUsername());
        return UserResponse.from(saved);
    }

    // ── Activate / Deactivate ────────────────────────────────────────────────

    @Transactional
    public UserResponse activateUser(Long id) {
        User user = findOrThrow(id);
        user.setActive(true);
        log.info("User activated: id={}", id);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(Long id) {
        User user = findOrThrow(id);
        user.setActive(false);
        log.info("User deactivated: id={}", id);
        return UserResponse.from(userRepository.save(user));
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void deleteUser(Long id) {
        User user = findOrThrow(id);

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException("CANNOT_DELETE_ADMIN", "Admin accounts cannot be deleted");
        }

        userRepository.delete(user);
        log.info("User deleted: id={}, username={}", id, user.getUsername());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Returns true when the currently authenticated principal has the
     * ADMIN_BRANCHES role but NOT ADMIN.  Used to enforce tighter role
     * restrictions for branch admins (e.g. they cannot assign CASHIER).
     */
    private boolean callerIsAdminBranchesOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        boolean hasAdminBranches = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN_BRANCHES"));
        boolean hasAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return hasAdminBranches && !hasAdmin;
    }

    public User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private Role parseAndValidateRole(String roleValue) {
        Role role;
        try {
            role = Role.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "INVALID_ROLE",
                    "Invalid role '" + roleValue + "'. Allowed: RECEPTION, CALL_CENTER, CASHIER, INVENTORY"
            );
        }

        if (!MANAGEABLE_ROLES.contains(role)) {
            throw new BusinessException(
                    "ROLE_NOT_ALLOWED",
                    "Role '" + roleValue + "' cannot be assigned via this API."
            );
        }

        return role;
    }

    private Branch resolveBranch(Role role, Long branchId) {
        if (BRANCH_REQUIRED_ROLES.contains(role)) {
            if (branchId == null) {
                throw new BusinessException(
                        "BRANCH_REQUIRED",
                        "A branch must be assigned for role: " + role.name()
                );
            }
            return branchService.findOrThrow(branchId);
        }
        return null;
    }

    private Shop resolveShop(Role role, Long shopId) {
        if (SHOP_REQUIRED_ROLES.contains(role)) {
            if (shopId == null) {
                throw new BusinessException(
                        "SHOP_REQUIRED",
                        "A shop must be assigned for role: " + role.name()
                );
            }
            return shopService.getOrThrow(shopId);
        }
        return null;
    }
}
