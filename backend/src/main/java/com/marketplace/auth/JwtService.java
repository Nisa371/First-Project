package com.marketplace.auth;

import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttl;
    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.ttl-seconds:3600}") long ttl, Environment environment) {
        if (ttl < 60 || ttl > 86400) throw new IllegalArgumentException("JWT lifetime must be 60–86400 seconds");
        this.ttl = ttl;
        if (secret.isBlank()) {
            if (environment.acceptsProfiles(Profiles.of("prod"))
                    || !environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                throw new IllegalStateException("JWT_SECRET is required outside dev/test");
            }
            // Ephemeral development key; restarting the backend invalidates local sessions.
            key = Jwts.SIG.HS256.key().build();
        } else {
            key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        }
    }
    public String issue(Long userId) {
        var now = Instant.now();
        return Jwts.builder().issuer("verified-career").subject(userId.toString())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(ttl)))
                .signWith(key, Jwts.SIG.HS256).compact();
    }
    public Long userId(String token) {
        return Long.valueOf(Jwts.parser().verifyWith(key).requireIssuer("verified-career")
                .build().parseSignedClaims(token).getPayload().getSubject());
    }
    public long ttlSeconds() { return ttl; }
}
