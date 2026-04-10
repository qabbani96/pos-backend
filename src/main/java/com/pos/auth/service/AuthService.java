package com.pos.auth.service;

import com.pos.auth.dto.LoginRequest;
import com.pos.auth.dto.LoginResponse;
import com.pos.auth.entity.User;
import com.pos.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Authenticates the user and returns a JWT token.
     * Throws BadCredentialsException if credentials are invalid (handled by GlobalExceptionHandler).
     */
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());

        Long branchId = user.getBranch() != null ? user.getBranch().getId() : null;

        return new LoginResponse(
                token,
                user.getRole().name(),
                user.getFullName(),
                user.getId(),
                branchId
        );
    }

    /**
     * Blacklists the token so it can no longer be used.
     */
    public void logout(String token) {
        jwtService.blacklistToken(token);
        log.info("Token blacklisted (logout)");
    }
}
