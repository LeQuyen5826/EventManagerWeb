package com.cmc.eventmanagement.config;

import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Luôn tạo 4 vai trò và tài khoản Admin (nếu chưa có).
 * Khi app.seed.enabled=true (profile dev) còn tạo tài khoản demo + danh mục + địa điểm để thử API ngay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    private static final String DEMO_PASSWORD = "Demo@12345";

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final EventCategoryRepository categoryRepo;
    private final VenueRepository venueRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedDemo;
    @Value("${app.seed.admin-email:admin@example.com}")
    private String adminEmail;
    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleName n : RoleName.values()) {
            if (roleRepo.findByName(n).isEmpty()) roleRepo.save(new Role(n));
        }
        createUserIfAbsent(adminEmail, "Quản trị viên", adminPassword, RoleName.ADMIN);

        if (seedDemo) {
            createUserIfAbsent("organizer@example.com", "Người tổ chức Demo", DEMO_PASSWORD, RoleName.ORGANIZER);
            createUserIfAbsent("staff@example.com", "Nhân viên Demo", DEMO_PASSWORD, RoleName.STAFF);
            createUserIfAbsent("attendee@example.com", "Người tham gia Demo", DEMO_PASSWORD, RoleName.ATTENDEE);
            if (categoryRepo.count() == 0) {
                categoryRepo.save(new EventCategory("Hội thảo"));
                categoryRepo.save(new EventCategory("Workshop"));
                categoryRepo.save(new EventCategory("Ngày hội việc làm"));
                categoryRepo.save(new EventCategory("Thể thao"));
            }
            if (venueRepo.count() == 0) {
                venueRepo.save(new Venue("Hội trường A", "Tòa nhà CMC, Hà Nội", 300));
                venueRepo.save(new Venue("Phòng Lab 101", "Tòa nhà CMC, Hà Nội", 60));
            }
            log.info("Đã nạp dữ liệu demo. Tài khoản: organizer@/staff@/attendee@example.com, mật khẩu {}", DEMO_PASSWORD);
        }
    }

    private void createUserIfAbsent(String email, String fullName, String rawPassword, RoleName role) {
        if (userRepo.existsByEmail(email)) return;
        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setRoles(new HashSet<>(Set.of(roleRepo.findByName(role).orElseThrow())));
        userRepo.save(u);
    }
}
