package com.cmc.eventmanagement.security;

import com.cmc.eventmanagement.config.AppProperties;
import com.cmc.eventmanagement.entity.Role;
import com.cmc.eventmanagement.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** Tạo và kiểm tra JWT (HMAC-SHA256). Khóa lấy từ cấu hình/biến môi trường JWT_SECRET. */
@Component
public class JwtService {
    private final AppProperties props;
    private final SecretKey key;

    public JwtService(AppProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(User u) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("uid", u.getId())
                .claim("roles", u.getRoles().stream().map(Role::getName).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(props.jwt().expirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Kiểm tra chữ ký và hạn; ném JwtException nếu token sai/hết hạn. */
    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }
}
