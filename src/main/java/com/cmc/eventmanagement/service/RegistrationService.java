package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.config.AppProperties;
import com.cmc.eventmanagement.dto.RegisterRequest;
import com.cmc.eventmanagement.dto.RegistrationResponse;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.event.RegistrationCancelledEvent;
import com.cmc.eventmanagement.event.RegistrationConfirmedEvent;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.*;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notificationRepo;
    private final QrCodeService qrService;
    private final ApplicationEventPublisher publisher;
    private final AppProperties props;

    /**
     * FR28-FR31 (Hình 3.3): khóa ghi sự kiện -> kiểm tra hạn đăng ký -> kiểm tra trùng -> kiểm tra sức chứa
     * -> lưu đăng ký + sinh QR -> gửi email sau commit.
     */
    @Transactional
    public RegistrationResponse register(Long eventId, RegisterRequest req, AppUserDetails me) {
        Event event = eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        if (event.getStatus() != EventStatus.PUBLISHED)
            throw new BusinessException("Sự kiện chưa mở đăng ký hoặc đã bị hủy");

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getRegistrationOpenAt())) throw new BusinessException("Chưa đến thời gian mở đăng ký");
        if (now.isAfter(event.getRegistrationCloseAt())) throw new BusinessException("Đã hết thời hạn đăng ký");

        // BR04: mỗi người một đăng ký/sự kiện. Đăng ký đã hủy thì được dùng lại dòng cũ (UNIQUE event+user).
        Registration reg = regRepo.findByEventIdAndUserId(eventId, me.getId()).orElse(null);
        if (reg != null && reg.getStatus() != RegistrationStatus.CANCELLED)
            throw new ConflictException("Bạn đã đăng ký sự kiện này");

        // BR03: sức chứa
        boolean full = regRepo.countSeatsTaken(eventId) >= event.getCapacity();
        if (full && !props.registration().waitlistEnabled())
            throw new ConflictException("Sự kiện đã hết chỗ");

        if (reg == null) {
            reg = new Registration();
            reg.setEvent(event);
            reg.setUser(userRepo.getReferenceById(me.getId()));
        }
        if (req != null) {
            reg.setStudentId(req.studentId());
            reg.setOrganization(req.organization());
        }
        if (full) reg.waitlist();                        // FR31: vào danh sách chờ
        else reg.confirm(qrService.newToken());          // FR28: xác nhận + sinh QR
        reg = regRepo.save(reg);

        if (reg.getStatus() == RegistrationStatus.CONFIRMED) {
            publisher.publishEvent(new RegistrationConfirmedEvent(reg.getId()));
        }
        return RegistrationResponse.from(reg);
    }

    /**
     * FR33, FR32, BR05: hủy đăng ký và đưa người đầu danh sách chờ lên. Tất cả cùng thành công hoặc cùng rollback.
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long registrationId, AppUserDetails me) {
        Registration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
        if (!reg.getUser().getId().equals(me.getId()))
            throw new AccessDeniedException("Không phải đăng ký của bạn");

        Event event = eventRepo.findByIdForUpdate(reg.getEvent().getId()).orElseThrow();   // khóa sự kiện
        if (reg.getStatus() == RegistrationStatus.CANCELLED || reg.getStatus() == RegistrationStatus.CHECKED_IN)
            throw new BusinessException("Đăng ký ở trạng thái hiện tại không thể hủy");
        event.assertCancellable(LocalDateTime.now());                 // BR05

        boolean freedSeat = reg.getStatus() == RegistrationStatus.CONFIRMED;
        reg.cancel();                                                  // 1. CANCELLED, QR mất hiệu lực (FR39)

        if (freedSeat) {
            regRepo.findFirstByEventIdAndStatusOrderByCreatedAtAsc(event.getId(), RegistrationStatus.WAITLISTED)
                    .ifPresent(next -> {
                        next.confirm(qrService.newToken());            // 2. người chờ lên CONFIRMED + sinh QR (FR32)
                        notificationRepo.save(Notification.promoted(next));   // 3. thông báo trong hệ thống (FR51)
                        publisher.publishEvent(new RegistrationConfirmedEvent(next.getId()));
                    });
        }
        publisher.publishEvent(new RegistrationCancelledEvent(reg.getId()));   // 4. email, gửi sau commit
    }

    /** Vé của tôi (kèm mã QR). */
    @Transactional(readOnly = true)
    public List<RegistrationResponse> myRegistrations(AppUserDetails me) {
        return regRepo.findByUserIdOrderByCreatedAtDesc(me.getId()).stream().map(RegistrationResponse::from).toList();
    }

    /** Organizer của sự kiện (hoặc ADMIN) xem danh sách đăng ký. */
    @Transactional(readOnly = true)
    public Page<RegistrationResponse> listByEvent(Long eventId, Pageable pageable, AppUserDetails me) {
        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        if (!me.isAdmin() && !event.isOrganizedBy(me.getId()))
            throw new AccessDeniedException("Không phải sự kiện của bạn");
        return regRepo.findByEventId(eventId, pageable).map(RegistrationResponse::forOrganizer);
    }

    /** Ảnh PNG mã QR của chính người đăng ký. */
    @Transactional(readOnly = true)
    public byte[] qrImage(Long registrationId, AppUserDetails me) {
        Registration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
        if (!reg.getUser().getId().equals(me.getId()))
            throw new AccessDeniedException("Không phải đăng ký của bạn");
        if (reg.getQrToken() == null)
            throw new BusinessException("Đăng ký này chưa có mã QR (đang chờ hoặc đã hủy)");
        return qrService.generatePng(reg.getQrToken(), 300);
    }
}
