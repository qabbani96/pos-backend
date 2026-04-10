package com.pos.auth.repository;

import com.pos.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Loads the user together with its Branch in a single LEFT JOIN query.
     *
     * Without @EntityGraph, accessing user.getBranch() outside an open session
     * (e.g. in AuthService.login() or InvoiceService after the filter chain
     * hands off the request) triggers a second SELECT — an N+1 on every
     * authenticated request.  This annotation fixes that.
     */
    @EntityGraph(attributePaths = "branch")
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
