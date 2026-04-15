package com.interviewprep.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms:900000}") long accessTokenExpiryMs,
            @Value("${app.jwt.refresh-token-expiry-ms:604800000}") long refreshTokenExpiryMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(
                Base64.getEncoder().encodeToString(secret.getBytes())
        ));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), accessTokenExpiryMs, "access");
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), refreshTokenExpiryMs, "refresh");
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            Claims claims = extractClaims(token);
            return claims.getSubject().equals(userDetails.getUsername())
                    && !isTokenExpired(claims);
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractClaims(token).get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMs / 1000;
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private String buildToken(String subject, long expiryMs, String type) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(signingKey)
                .compact();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
