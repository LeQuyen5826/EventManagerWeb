package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.config.AppProperties;
import com.cmc.eventmanagement.dto.CheckInResponse;
import com.cmc.eventmanagement.dto.ScanRequest;
import com.cmc.eventmanagement.entity.CheckIn;
import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.entity.Registration;
import com.cmc.eventmanagement.entity.RegistrationStatus;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.CheckInRepository;
import com.cmc.eventmanagement.repository.RegistrationRepository;
import com.cmc.eventmanagement.repository.UserRepository;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInService {
    private final RegistrationRepository regRepo;      // tra cứu đăng ký theo qrToken
    private final CheckInRepository checkInRepo;       // lưu lượt check-in
    private final EventStaffGuard assignmentGuard;     // kiểm tra nhân viên được phân công (BR07)
    private final AuditLogService auditLog;            // ghi nhật ký thao tác (FR58)
    private final UserRepository userRepo;
    private final AppProperties props;

    /** FR40: check-in bằng QR. Khóa dòng đăng ký để quét trùng đồng thời không check-in hai lần. */
    @Transactional
    public CheckInResponse scan(ScanRequest req, AppUserDetails me) {
        Registration reg = regRepo.findByQrTokenForUpdate(req.qrToken())
                .orElseThrow(() -> new NotFoundException("Mã QR không hợp lệ hoặc đã mất hiệu lực"));
        Event event = reg.getEvent();

        assignmentGuard.assertAssigned(event.getId(), me);                       // BR07 -> 403

        if (reg.getStatus() == RegistrationStatus.CHECKED_IN)
            throw new ConflictException("Mã QR đã được check-in");               // 409
        if (reg.getStatus() != RegistrationStatus.CONFIRMED)
            throw new BusinessException("Đăng ký không ở trạng thái được phép check-in");

        if (props.checkin().enforceWindow()) {                                   // BR06
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(event.getStartAt().minusMinutes(props.checkin().earlyMinutes())))
                throw new BusinessException("Chưa đến thời gian check-in");
            if (now.isAfter(event.getEndAt()))
                throw new BusinessException("Sự kiện đã kết thúc, không thể check-in");
        }

        reg.checkIn();
        CheckIn c = new CheckIn();
        c.setRegistration(reg);
        c.setCheckedInAt(LocalDateTime.now());
        c.setCheckedInBy(userRepo.getReferenceById(me.getId()));
        checkInRepo.save(c);

        auditLog.log(me.getId(), "CHECK_IN", "registrationId=" + reg.getId() + ", eventId=" + event.getId());
        return new CheckInResponse(reg.getId(), event.getTitle(), reg.getUser().getFullName(),
                reg.getStudentId(), c.getCheckedInAt());
    }
}
