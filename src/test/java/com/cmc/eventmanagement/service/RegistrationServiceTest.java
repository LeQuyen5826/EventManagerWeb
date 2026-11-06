package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.request.RegisterRequest;
import com.cmc.eventmanagement.dto.response.RegistrationResponse;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.event.RegistrationConfirmedEvent;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.mapper.RegistrationMapper;
import com.cmc.eventmanagement.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Kiểm thử nghiệp vụ đăng ký bằng Mockito: không cần MySQL (lợi ích của Dependency Injection). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceTest {
    @Mock EventRepository eventRepo;
    @Mock RegistrationRepository regRepo;
    @Mock UserRepository userRepo;
    @Mock NotificationRepository notifRepo;
    @Mock EventStaffGuard guard;
    @Mock AuditLogService auditLog;
    @Mock ApplicationEventPublisher publisher;

    RegistrationService service;
    Event event;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(eventRepo, regRepo, userRepo, notifRepo,
                new QrCodeService(), new RegistrationMapper(), guard, auditLog, publisher);

        event = new Event();
        event.setTitle("Hội thảo AI");
        event.setCapacity(1);
        event.setStatus(EventStatus.PUBLISHED);
        event.setStartAt(LocalDateTime.now().plusDays(10));
        event.setEndAt(LocalDateTime.now().plusDays(10).plusHours(3));
        event.setRegistrationOpenAt(LocalDateTime.now().minusDays(1));
        event.setRegistrationCloseAt(LocalDateTime.now().plusDays(5));

        when(eventRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(event));
        when(regRepo.findByEventIdAndUserId(1L, 7L)).thenReturn(Optional.empty());
        when(regRepo.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void hetCho_khongVaoDanhSachCho_nemConflict() {
        when(regRepo.countSeatsTaken(1L)).thenReturn(1L);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.register(1L, 7L, new RegisterRequest(null, null, false)));

        assertEquals("Sự kiện đã hết chỗ", ex.getMessage());
        verify(regRepo, never()).save(any());
    }

    @Test
    void daDangKy_nemConflict() {
        Registration existing = new Registration();
        existing.setStatus(RegistrationStatus.CONFIRMED);
        when(regRepo.findByEventIdAndUserId(1L, 7L)).thenReturn(Optional.of(existing));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.register(1L, 7L, new RegisterRequest(null, null, false)));

        assertEquals("Bạn đã đăng ký sự kiện này", ex.getMessage());
    }

    @Test
    void conCho_xacNhanVaSinhQr_phatSuKienGuiEmail() {
        when(regRepo.countSeatsTaken(1L)).thenReturn(0L);

        RegistrationResponse res = service.register(1L, 7L, new RegisterRequest("BIT240201", "24IT2", false));

        assertEquals(RegistrationStatus.CONFIRMED, res.status());
        assertNotNull(res.qrToken());
        assertEquals(64, res.qrToken().length());
        verify(publisher).publishEvent(any(RegistrationConfirmedEvent.class));
    }

    @Test
    void hetCho_chonDanhSachCho_khongCoQr() {
        when(regRepo.countSeatsTaken(1L)).thenReturn(1L);

        RegistrationResponse res = service.register(1L, 7L, new RegisterRequest(null, null, true));

        assertEquals(RegistrationStatus.WAITLISTED, res.status());
        assertNull(res.qrToken());
        verify(publisher, never()).publishEvent(any(RegistrationConfirmedEvent.class));
    }
}
