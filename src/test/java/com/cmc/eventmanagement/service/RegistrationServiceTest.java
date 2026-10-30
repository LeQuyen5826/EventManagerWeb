package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.config.AppProperties;
import com.cmc.eventmanagement.dto.RegisterRequest;
import com.cmc.eventmanagement.dto.RegistrationResponse;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.event.RegistrationConfirmedEvent;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.repository.*;
import com.cmc.eventmanagement.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm thử nghiệp vụ đăng ký bằng Mockito: giả lập Repository nên không cần MySQL (mục 3.2a). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceTest {
    @Mock EventRepository eventRepo;
    @Mock RegistrationRepository regRepo;
    @Mock UserRepository userRepo;
    @Mock NotificationRepository notificationRepo;
    @Mock QrCodeService qrService;
    @Mock ApplicationEventPublisher publisher;

    private Event event;
    private final AppUserDetails me =
            new AppUserDetails(5L, "a@example.com", "hash", true, List.of(new SimpleGrantedAuthority("ROLE_ATTENDEE")));

    private RegistrationService service(boolean waitlist) {
        AppProperties props = new AppProperties(new AppProperties.Jwt("x".repeat(40), 120),
                new AppProperties.Registration(waitlist), new AppProperties.Checkin(true, 120),
                new AppProperties.Cors(List.of()));
        return new RegistrationService(eventRepo, regRepo, userRepo, notificationRepo, qrService, publisher, props);
    }

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(1L);
        event.setTitle("Hội thảo AI");
        event.setCapacity(1);
        event.setStatus(EventStatus.PUBLISHED);
        event.setRegistrationOpenAt(LocalDateTime.now().minusDays(1));
        event.setRegistrationCloseAt(LocalDateTime.now().plusDays(1));
        when(eventRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(regRepo.findByEventIdAndUserId(1L, 5L)).thenReturn(Optional.empty());

        User u = new User();
        u.setId(5L);
        u.setFullName("A");
        u.setEmail("a@example.com");
        when(userRepo.getReferenceById(5L)).thenReturn(u);
        when(regRepo.save(any(Registration.class))).thenAnswer(i -> i.getArgument(0));
        when(qrService.newToken()).thenReturn("tok123");
    }

    @Test
    void hetCho_vaTatDanhSachCho_nemConflict() {
        when(regRepo.countSeatsTaken(1L)).thenReturn(1L);
        ConflictException ex = assertThrows(ConflictException.class,
                () -> service(false).register(1L, new RegisterRequest("BIT1", "24IT2"), me));
        assertEquals("Sự kiện đã hết chỗ", ex.getMessage());
    }

    @Test
    void hetCho_vaBatDanhSachCho_vaoWaitlistKhongCoQr() {
        when(regRepo.countSeatsTaken(1L)).thenReturn(1L);
        RegistrationResponse res = service(true).register(1L, null, me);
        assertEquals(RegistrationStatus.WAITLISTED, res.status());
        assertNull(res.qrToken());
    }

    @Test
    void conCho_xacNhanVaCoQr() {
        when(regRepo.countSeatsTaken(1L)).thenReturn(0L);
        RegistrationResponse res = service(false).register(1L, new RegisterRequest("BIT1", "24IT2"), me);
        assertEquals(RegistrationStatus.CONFIRMED, res.status());
        assertEquals("tok123", res.qrToken());
        verify(publisher).publishEvent(any(RegistrationConfirmedEvent.class));
    }

    @Test
    void dangKyTrung_nemConflict() {
        Registration existing = new Registration();
        existing.setStatus(RegistrationStatus.CONFIRMED);
        when(regRepo.findByEventIdAndUserId(1L, 5L)).thenReturn(Optional.of(existing));
        assertThrows(ConflictException.class, () -> service(false).register(1L, null, me));
    }

    @Test
    void ngoaiThoiGianDangKy_nemBusinessException() {
        event.setRegistrationCloseAt(LocalDateTime.now().minusHours(1));
        assertThrows(BusinessException.class, () -> service(false).register(1L, null, me));
    }
}
