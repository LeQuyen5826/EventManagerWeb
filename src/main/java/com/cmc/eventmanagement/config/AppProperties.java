package com.cmc.eventmanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Cấu hình tùy chỉnh của ứng dụng (tiền tố "app" trong application.yml). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Registration registration, Checkin checkin, Cors cors) {
    public record Jwt(String secret, long expirationMinutes) {}
    public record Registration(boolean waitlistEnabled) {}
    public record Checkin(boolean enforceWindow, long earlyMinutes) {}
    public record Cors(List<String> allowedOrigins) {}
}
