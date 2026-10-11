package com.aimmaster.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Builds and reads the JWT ("JSON Web Token") that proves a request came from a
 * logged-in player, without the server having to remember a session in memory.
 * A JWT is just a signed, tamper-proof string: "username=alice, expires=<time>",
 * signed with a secret key only our server knows. Anyone can READ it, but only
 * we can produce one with a valid signature - that's what makes it trustworthy.
 *
 * "@Service" tells Spring "this class holds business logic, create and manage one
 * instance of it for the whole app" - we never write `new JwtService()` ourselves.
 */
@Service
public class JwtService {

    @Value("${aimmaster.jwt.secret}")
    private String secret;

    @Value("${aimmaster.jwt.expiration-ms}")
    private long expirationMs;

    private Key signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Creates a new token for this username, valid for aimmaster.jwt.expiration-ms milliseconds. */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Reads the username back out of a token. Only call this after isTokenValid() returns true. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** True if the token's signature checks out AND it hasn't expired yet. */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // Covers: bad signature, malformed token, expired token, null/blank token.
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
