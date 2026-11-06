package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.response.CheckInResponse;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.CheckInRepository;
import com.cmc.eventmanagement.repository.EventRepository;
import com.cmc.eventmanagement.repository.RegistrationRepository;
import com.cmc.eventmanagement.repository.UserRepository;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckInService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final CheckInRepository checkInRepo;
    private final UserRepository userRepo;
    private final EventStaffGuard assignmentGuard;       // BR07
    private final AuditLogService auditLog;               // FR58

    @Value("${app.event.checkin-open-before-minutes:60}")
    private int openBeforeMinutes;

    /** FR40, FR41: quét QR. Khóa dòng đăng ký để hai lượt quét đồng thời không cùng thành công. */
    @Transactional
    public CheckInResponse scan(String qrToken, AppUserDetails actor) {
        Registration reg = regRepo.findByQrTokenForUpdate(qrToken)
                .orElseThrow(() -> new NotFoundException("Mã QR không hợp lệ"));
        return doCheckIn(reg, actor, false);
    }

    /** FR42: check-in thủ công khi người tham gia không có mã. */
    @Transactional
    public CheckInResponse manual(Long registrationId, AppUserDetails actor) {
        Registration reg = regRepo.findByIdForUpdate(registrationId)
                .orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
        return doCheckIn(reg, actor, true);
    }

    /** FR43: hoàn tác check-in nhầm (chủ sự kiện/Admin). */
    @Transactional
    public void undo(Long registrationId, AppUserDetails actor) {
        Registration reg = regRepo.findByIdForUpdate(registrationId)
                .orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
        Event event = reg.getEvent();
        if (!(actor.isAdmin() || event.isOwnedBy(actor.getId()))) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ người tổ chức mới hoàn tác được");
        }
        if (reg.getStatus() != RegistrationStatus.CHECKED_IN) throw new BusinessException("Đăng ký chưa được check-in");
        checkInRepo.findByRegistrationId(registrationId).ifPresent(checkInRepo::delete);
        reg.undoCheckIn();
        auditLog.log(actor.getId(), "CHECKIN_UNDO", "registrationId=" + registrationId);
    }

    /** FR44: số đã check-in so với tổng đăng ký. */
    @Transactional(readOnly = true)
    public Map<String, Long> stats(Long eventId, AppUserDetails actor) {
        Event event = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        assignmentGuard.assertCanView(event, actor);
        long checkedIn = regRepo.countByEventAndStatuses(eventId, List.of(RegistrationStatus.CHECKED_IN));
        long registered = regRepo.countByEventAndStatuses(eventId,
                List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.CHECKED_IN));
        return Map.of("registered", registered, "checkedIn", checkedIn);
    }

    private CheckInResponse doCheckIn(Registration reg, AppUserDetails actor, boolean manual) {
        Event event = reg.getEvent();
        assignmentGuard.assertCanCheckIn(event, actor);                                   // BR07

        if (event.getStatus() == EventStatus.CANCELLED) throw new BusinessException("Sự kiện đã bị hủy");   // FR39
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(event.getStartAt().minusMinutes(openBeforeMinutes)) || now.isAfter(event.getEndAt())) {
            throw new BusinessException("Ngoài khoảng thời gian cho phép check-in");      // BR06
        }

        switch (reg.getStatus()) {
            case CHECKED_IN -> {                                                          // FR41
                CheckIn old = checkInRepo.findByRegistrationId(reg.getId()).orElse(null);
                String when = old == null ? "" : " lúc " + old.getCheckedInAt().format(FMT);
                String by = (old == null || old.getCheckedInBy() == null) ? "" : " bởi " + old.getCheckedInBy().getFullName();
                throw new ConflictException("Đã check-in" + when + by);
            }
            case CONFIRMED -> { /* hợp lệ */ }
            case CANCELLED -> throw new BusinessException("Đăng ký đã bị hủy");
            default -> throw new BusinessException("Đăng ký không hợp lệ để check-in (" + reg.getStatus() + ")");
        }

        User staff = userRepo.findById(actor.getId()).orElseThrow();
        CheckIn ci = new CheckIn();
        ci.setRegistration(reg);
        ci.setCheckedInAt(now);
        ci.setCheckedInBy(staff);
        ci.setManual(manual);
        checkInRepo.save(ci);
        reg.markCheckedIn();
        auditLog.log(actor.getId(), manual ? "CHECKIN_MANUAL" : "CHECKIN_QR", "registrationId=" + reg.getId());

        User u = reg.getUser();
        return new CheckInResponse(reg.getId(), u.getFullName(), u.getEmail(), u.getAvatarUrl(),
                event.getTitle(), now, staff.getFullName());
    }
}
