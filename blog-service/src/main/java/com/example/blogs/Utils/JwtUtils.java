package com.example.blogs.Utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class JwtUtils {

    private final Key key;
    private final long defaultTtlSeconds;

    public JwtUtils(String base64Secret, long defaultTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public String generateToken(UUID userId, List<String> roles, Long ttlSecondsOverride) {
        long ttl = ttlSecondsOverride != null ? ttlSecondsOverride : defaultTtlSeconds;
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttl)))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public ParsedToken parseAndValidate(String jwt) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(jwt);

        Claims c = jws.getBody();
        UUID userId = UUID.fromString(c.getSubject());

        // Handle both "roles" (plural, List) and "role" (singular, String)
        List<String> roles;
        Object rolesObj = c.get("roles");
        Object roleObj = c.get("role");

        if (rolesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> rolesList = (List<String>) rolesObj;
            roles = rolesList;
        } else if (roleObj instanceof String) {
            // Convert singular "role" claim to a list
            roles = List.of((String) roleObj);
        } else {
            // Default to empty list if neither claim exists
            roles = List.of();
        }

        return new ParsedToken(userId, roles);
    }

    public record ParsedToken(UUID userId, List<String> roles) {
    }
}