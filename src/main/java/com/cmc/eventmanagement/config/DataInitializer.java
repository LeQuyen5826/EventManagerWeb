package com.cmc.eventmanagement.config;

import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/** Dữ liệu demo: vai trò, 4 tài khoản, loại sự kiện, địa điểm, 2 sự kiện và 1 vé mẫu để thử check-in. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {
    public static final String DEMO_QR_TOKEN = "DEMO-QR-TOKEN-0001";

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final VenueRepository venueRepo;
    private final EventCategoryRepository categoryRepo;
    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (roleRepo.count() > 0) return;

        Map<String, Role> roles = Map.of(
                "ADMIN", roleRepo.save(new Role("ADMIN")),
                "ORGANIZER", roleRepo.save(new Role("ORGANIZER")),
                "ATTENDEE", roleRepo.save(new Role("ATTENDEE")),
                "STAFF", roleRepo.save(new Role("STAFF")));

        createUser("Quản trị viên", "admin@example.com", "Admin@123", roles.get("ADMIN"));
        User organizer = createUser("Nguyễn Văn Tổ Chức", "organizer@example.com", "Abc@12345", roles.get("ORGANIZER"));
        User attendee = createUser("Trần Thị Tham Dự", "attendee@example.com", "Abc@12345", roles.get("ATTENDEE"));
        User staff = createUser("Lê Văn Nhân Viên", "staff@example.com", "Abc@12345", roles.get("STAFF"));

        EventCategory seminar = categoryRepo.save(new EventCategory("Hội thảo"));
        categoryRepo.save(new EventCategory("Workshop"));
        categoryRepo.save(new EventCategory("Cuộc thi"));
        Venue hall = venueRepo.save(new Venue("Hội trường A", "Tầng 1, Tòa nhà chính", 200));
        venueRepo.save(new Venue("Phòng họp B", "Tầng 3, Tòa nhà chính", 50));

        LocalDateTime now = LocalDateTime.now();

        // Sự kiện 1: sắp diễn ra, đang mở đăng ký
        Event upcoming = newEvent("Hội thảo AI trong giáo dục", seminar, hall, organizer, 150,
                now.plusDays(30), now.plusDays(30).plusHours(3), now.minusDays(1), now.plusDays(29));
        upcoming.getStaff().add(staff);
        eventRepo.save(upcoming);

        // Sự kiện 2: đang diễn ra, dùng để thử check-in
        Event live = newEvent("Workshop Spring Boot (đang diễn ra)", seminar, hall, organizer, 2,
                now.minusHours(1), now.plusHours(3), now.minusDays(7), now.minusHours(2));
        live.getStaff().add(staff);
        eventRepo.save(live);

        Registration demo = new Registration();
        demo.setEvent(live);
        demo.setUser(attendee);
        demo.setStudentId("BIT240000");
        demo.setOrganization("24IT2");
        demo.confirm(DEMO_QR_TOKEN);
        regRepo.save(demo);

        log.info("Đã tạo dữ liệu demo. Tài khoản: admin@example.com/Admin@123; organizer@, attendee@, staff@example.com / Abc@12345");
        log.info("Mã QR demo để thử check-in: {}", DEMO_QR_TOKEN);
    }

    private User createUser(String name, String email, String rawPassword, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.getRoles().add(role);
        return userRepo.save(u);
    }

    private Event newEvent(String title, EventCategory cat, Venue venue, User organizer, int capacity,
                           LocalDateTime start, LocalDateTime end, LocalDateTime openAt, LocalDateTime closeAt) {
        Event e = new Event();
        e.setTitle(title);
        e.setDescription("Sự kiện demo của hệ thống quản lý sự kiện");
        e.setCategory(cat);
        e.setVenue(venue);
        e.setOrganizer(organizer);
        e.setCapacity(capacity);
        e.setStartAt(start);
        e.setEndAt(end);
        e.setRegistrationOpenAt(openAt);
        e.setRegistrationCloseAt(closeAt);
        e.setStatus(EventStatus.PUBLISHED);
        return e;
    }
}
