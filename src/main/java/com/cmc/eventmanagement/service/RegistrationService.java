package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.request.RegisterRequest;
import com.cmc.eventmanagement.dto.response.AttendeeResponse;
import com.cmc.eventmanagement.dto.response.RegistrationResponse;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.event.RegistrationCancelledEvent;
import com.cmc.eventmanagement.event.RegistrationConfirmedEvent;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.mapper.RegistrationMapper;
import com.cmc.eventmanagement.repository.*;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final NotificationRepository notifRepo;
    private final QrCodeService qrService;
    private final RegistrationMapper mapper;
    private final EventStaffGuard guard;
    private final AuditLogService auditLog;
    private final ApplicationEventPublisher publisher;

    @Value("${app.event.cancel-before-hours:24}")
    private int cancelBeforeHours;

    /**
     * FR28-FR30, FR37, FR47. Khóa ghi dòng sự kiện để các request đăng ký đồng thời xếp hàng tuần tự,
     * nhờ đó đếm chỗ + ghi đăng ký không bao giờ vượt sức chứa (BR03).
     */
    @Transactional
    public RegistrationResponse register(Long eventId, Long userId, RegisterRequest req) {
        RegisterRequest r = (req == null) ? new RegisterRequest(null, null, null) : req;

        Event event = eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessException("Sự kiện chưa mở đăng ký hoặc đã đóng");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getRegistrationOpenAt())) throw new BusinessException("Chưa đến thời gian mở đăng ký");
        if (now.isAfter(event.getRegistrationCloseAt())) throw new BusinessException("Đã hết hạn đăng ký");

        Registration reg = regRepo.findByEventIdAndUserId(eventId, userId).orElse(null);
        if (reg != null && reg.getStatus() != RegistrationStatus.CANCELLED) {          // BR04, FR29
            throw new ConflictException("Bạn đã đăng ký sự kiện này");
        }

        boolean full = regRepo.countSeatsTaken(eventId) >= event.getCapacity();       // BR03
        if (full && !Boolean.TRUE.equals(r.joinWaitlist())) {
            throw new ConflictException("Sự kiện đã hết chỗ");
        }

        User user = userRepo.getReferenceById(userId);
        if (reg == null) {
            reg = new Registration();
            reg.setEvent(event);
            reg.setUser(user);
        }
        reg.setStudentId(r.studentId());
        reg.setOrganization(r.organization());
        reg.setCreatedAt(now);
        if (full) {
            reg.setStatus(RegistrationStatus.WAITLISTED);      // FR32
            reg.setQrToken(null);
        } else {
            reg.confirm(qrService.newToken());                 // FR37
        }
        reg = regRepo.save(reg);

        if (full) {
            notifRepo.save(Notification.of(user, "Bạn đang trong danh sách chờ",
                    "Sự kiện \"" + event.getTitle() + "\" đã hết chỗ. Bạn sẽ được thông báo khi có chỗ trống."));
        } else {
            notifRepo.save(Notification.of(user, "Đăng ký thành công",
                    "Bạn đã đăng ký sự kiện \"" + event.getTitle() + "\". Mã QR của bạn có trong mục Vé của tôi."));
            publisher.publishEvent(new RegistrationConfirmedEvent(reg.getId()));     // email gửi sau commit
        }
        return mapper.toResponse(reg);
    }

    /** FR33, FR32, BR05: hủy đăng ký + chuyển người đầu danh sách chờ lên. Tất cả cùng thành công hoặc cùng rollback. */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long registrationId, Long userId) {
        Registration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
        if (!reg.getUser().getId().equals(userId)) throw new AccessDeniedException("Không phải đăng ký của bạn");
        Event event = eventRepo.findByIdForUpdate(reg.getEvent().getId()).orElseThrow();   // khóa sự kiện
        if (reg.getStatus() == RegistrationStatus.CANCELLED) throw new BusinessException("Đăng ký đã được hủy trước đó");
        event.assertCancellable(LocalDateTime.now(), cancelBeforeHours);                   // BR05
        releaseAndPromote(reg, event);
    }

    /** FR35: người tổ chức/Admin hủy đăng ký của một người (không bị giới hạn BR05). */
    @Transactional(rollbackFor = Exception.class)
    public void cancelByOrganizer(Long eventId, Long registrationId, AppUserDetails actor) {
        Event event = eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        guard.assertCanManage(event, actor);
        Registration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
        if (!reg.getEvent().getId().equals(eventId)) throw new NotFoundException("Đăng ký không thuộc sự kiện này");
        if (reg.getStatus() == RegistrationStatus.CANCELLED) throw new BusinessException("Đăng ký đã được hủy trước đó");
        releaseAndPromote(reg, event);
        auditLog.log(actor.getId(), "REGISTRATION_REMOVE", "eventId=" + eventId + ", registrationId=" + registrationId);
    }

    private void releaseAndPromote(Registration reg, Event event) {
        boolean heldSeat = reg.getStatus() == RegistrationStatus.CONFIRMED;
        reg.cancel();                                                       // 1. CANCELLED, QR mất hiệu lực (FR39)
        if (heldSeat) {
            regRepo.findFirstByEventIdAndStatusOrderByCreatedAtAsc(event.getId(), RegistrationStatus.WAITLISTED)
                    .ifPresent(next -> {
                        next.confirm(qrService.newToken());                 // 2. người chờ lên CONFIRMED + sinh QR
                        notifRepo.save(Notification.promoted(next));        // 3. thông báo trong hệ thống (FR51)
                        publisher.publishEvent(new RegistrationConfirmedEvent(next.getId()));   // 4. email sau commit
                    });
        }
        publisher.publishEvent(new RegistrationCancelledEvent(reg.getId()));
    }

    /** FR36: sự kiện của tôi. */
    @Transactional(readOnly = true)
    public List<RegistrationResponse> myRegistrations(Long userId) {
        return regRepo.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toResponse).toList();
    }

    /** FR35: danh sách đăng ký của một sự kiện (chủ sự kiện, nhân viên được phân công, Admin). */
    @Transactional(readOnly = true)
    public Page<AttendeeResponse> listAttendees(Long eventId, RegistrationStatus status, String keyword,
                                                Pageable pageable, AppUserDetails actor) {
        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        guard.assertCanView(event, actor);
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return regRepo.searchByEvent(eventId, status, kw, pageable).map(mapper::toAttendee);
    }

    /** FR38: ảnh QR của chính mình (chỉ khi đăng ký còn hiệu lực). */
    @Transactional(readOnly = true)
    public byte[] qrPng(Long registrationId, Long userId) {
        Registration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
        if (!reg.getUser().getId().equals(userId)) throw new AccessDeniedException("Không phải đăng ký của bạn");
        if (reg.getQrToken() == null || (reg.getStatus() != RegistrationStatus.CONFIRMED
                && reg.getStatus() != RegistrationStatus.CHECKED_IN)) {
            throw new BusinessException("Đăng ký này không có mã QR hợp lệ");
        }
        return qrService.generatePng(reg.getQrToken(), 300);
    }
}
