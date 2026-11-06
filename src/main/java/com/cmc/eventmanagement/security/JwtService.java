package com.cmc.eventmanagement.security;

import com.cmc.eventmanagement.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {
    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        // khóa HMAC lấy từ biến môi trường JWT_SECRET (>= 32 ký tự), không commit lên Git
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(User u) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("uid", u.getId())
                .claim("roles", u.getRoles().stream().map(r -> r.getName().name()).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.expirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Kiểm tra chữ ký và hạn; ném JwtException nếu token sai/hết hạn. */
    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public long expiresInSeconds() { return props.expirationMinutes() * 60; }
}
