package com.cmc.eventmanagement.config;

import com.cmc.eventmanagement.security.JwtAuthenticationFilter;
import com.cmc.eventmanagement.security.RestAccessDeniedHandler;
import com.cmc.eventmanagement.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler deniedHandler;
    private final AppProperties props;

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())          // token nằm trong header Authorization, không dùng cookie phiên
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                // công khai
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/*", "/api/categories", "/api/venues").permitAll()
                // ORGANIZER
                .requestMatchers(HttpMethod.POST, "/api/events", "/api/events/*/staff").hasRole("ORGANIZER")
                // ORGANIZER (chủ sự kiện, kiểm tra ở Service) hoặc ADMIN
                .requestMatchers(HttpMethod.POST, "/api/events/*/publish", "/api/events/*/cancel").hasAnyRole("ORGANIZER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/events/*/report", "/api/events/*/registrations").hasAnyRole("ORGANIZER", "ADMIN")
                // ATTENDEE
                .requestMatchers(HttpMethod.POST, "/api/events/*/registrations").hasRole("ATTENDEE")
                .requestMatchers(HttpMethod.DELETE, "/api/registrations/*").hasRole("ATTENDEE")
                .requestMatchers(HttpMethod.GET, "/api/registrations/*/qr").hasRole("ATTENDEE")
                // STAFF, ORGANIZER
                .requestMatchers("/api/checkin/**").hasAnyRole("STAFF", "ORGANIZER")
                // ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // còn lại: chỉ cần đăng nhập (/api/me/**)
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** CORS: chỉ cho phép origin của frontend. */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.cors().allowedOrigins());
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
