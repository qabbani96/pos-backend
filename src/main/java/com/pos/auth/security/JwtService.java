package com.pos.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * In-memory token blacklist: JTI → expiry time.
     * Thread-safe. Entries are cleaned up hourly to prevent memory growth.
     * Note: blacklist resets on server restart — acceptable for MVP.
     */
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    // ─── Token generation ─────────────────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        return Jwts.builder()
                .id(UUID.randomUUID().toString())      // JTI — used for blacklisting
                .subject(userDetails.getUsername())
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    // ─── Token validation ─────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token)
                && !isBlacklisted(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ─── Blacklist (logout) ───────────────────────────────────────────────────

    public void blacklistToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            blacklist.put(claims.getId(), claims.getExpiration());
            log.debug("Token blacklisted for user: {}", claims.getSubject());
        } catch (Exception e) {
            log.warn("Could not blacklist token: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String jti = extractClaim(token, Claims::getId);
            return blacklist.containsKey(jti);
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    /** Remove expired entries from the blacklist every hour. */
    @Scheduled(fixedRate = 3_600_000)
    public void cleanBlacklist() {
        Date now = new Date();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue().before(now));
        log.debug("Blacklist cleanup: removed {} expired entries. Remaining: {}", before - blacklist.size(), blacklist.size());
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
