package com.marketplace.auth;

import java.time.Instant;
import java.util.Date;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    @Test void rejectsExpiredAndWrongSignatureTokens() {
        var key = Jwts.SIG.HS256.key().build();
        var service = new JwtService(Encoders.BASE64.encode(key.getEncoded()), 3600, new MockEnvironment());
        String expired = Jwts.builder().issuer("verified-career").subject("1")
                .expiration(Date.from(Instant.now().minusSeconds(10))).signWith(key).compact();
        assertThatThrownBy(() -> service.userId(expired)).isInstanceOf(ExpiredJwtException.class);
        var other = new JwtService("",3600,new MockEnvironment().withProperty("spring.profiles.active","test"));
        assertThatThrownBy(() -> service.userId(other.issue(1L))).isInstanceOf(io.jsonwebtoken.JwtException.class);
        assertThat(service.userId(service.issue(4L))).isEqualTo(4L);
    }
    @Test void productionRequiresSecretEvenWithDevEnabled() {
        var env = new MockEnvironment(); env.setActiveProfiles("prod","dev");
        assertThatThrownBy(() -> new JwtService("",3600,env)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService("short",3600,env)).isInstanceOf(RuntimeException.class);
    }
}
