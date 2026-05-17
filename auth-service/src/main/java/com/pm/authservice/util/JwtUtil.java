package com.pm.authservice.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret:}") String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("jwt.secret must be set (env var JWT_SECRET or property jwt.secret)");
        }

        // Defensive cleanup
        String s = secret.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }

        SecretKey tmp;
        try {
            // Try Base64 decode first
            byte[] decoded = Base64.getDecoder().decode(s);
            tmp = Keys.hmacShaKeyFor(decoded);
        } catch (IllegalArgumentException ex) {
            // Fallback: use raw UTF-8 bytes
            tmp = Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8));
        }
        this.key = tmp;
    }

    public String generateToken(String email, String role) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(60 * 60 * 10); // 10 hours

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public void validateToken(String token) {
        try {
            Jwts.parser().verifyWith((SecretKey) key)
                    .build()
                    .parseSignedClaims(token);
        } catch (SignatureException e) {
            throw new JwtException("Invalid JWT signature");
        } catch (JwtException e) {
            throw new JwtException("Invalid JWT");
        }
    }
}
