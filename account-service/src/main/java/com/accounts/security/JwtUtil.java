package com.accounts.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    private Key key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Object r = claims(token).get("roles");
        if (r instanceof List) {
            return (List<String>) r;
        } else if (r instanceof String) {
            return List.of((String) r);
        }
        return Collections.emptyList();
    }

    public Long extractCustomerId(String token) {
        Object cid = claims(token).get("customerId");
        if (cid == null) return null;
        if (cid instanceof Number) return ((Number) cid).longValue();
        try { return Long.valueOf(String.valueOf(cid)); } catch (Exception e) { return null; }
    }

    public boolean validate(String token) {
        try {
            claims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

