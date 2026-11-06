package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.response.NotificationResponse;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.NotificationRepository;
import com.cmc.eventmanagement.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final NotificationRepository notifRepo;
    private final RegistrationRepository regRepo;
    private final EmailSender emailSender;
    private final QrCodeService qrService;

    /** FR51: thông báo trong hệ thống. Tham gia transaction của nơi gọi. */
    @Transactional
    public void createInApp(User user, String title, String content) {
        notifRepo.save(Notification.of(user, title, content));
    }

    /** FR47: email xác nhận kèm ảnh QR. Được gọi bất đồng bộ sau khi commit. */
    @Transactional(readOnly = true)
    public void sendConfirmationEmail(Long registrationId) {
        Registration r = regRepo.findById(registrationId).orElse(null);
        if (r == null || r.getQrToken() == null || r.getStatus() != RegistrationStatus.CONFIRMED) return;
        Event e = r.getEvent();
        String text = "Xin chào " + r.getUser().getFullName() + ",\n\n"
                + "Bạn đã đăng ký thành công sự kiện \"" + e.getTitle() + "\".\n"
                + "Thời gian: " + e.getStartAt().format(FMT) + " - " + e.getEndAt().format(FMT) + "\n"
                + "Địa điểm: " + e.getVenue().getName() + "\n\n"
                + "Vui lòng mang theo mã QR đính kèm để check-in tại cửa.\n";
        emailSender.send(r.getUser().getEmail(), "Xác nhận đăng ký: " + e.getTitle(), text,
                qrService.generatePng(r.getQrToken(), 300));
    }

    @Transactional(readOnly = true)
    public void sendCancellationEmail(Long registrationId) {
        Registration r = regRepo.findById(registrationId).orElse(null);
        if (r == null) return;
        emailSender.send(r.getUser().getEmail(), "Đã hủy đăng ký: " + r.getEvent().getTitle(),
                "Đăng ký của bạn cho sự kiện \"" + r.getEvent().getTitle() + "\" đã được hủy. Mã QR cũ không còn hiệu lực.",
                null);
    }

    /** FR48: sự kiện thay đổi/hủy -> thông báo cho mọi người đã đăng ký hoặc đang chờ. */
    @Transactional
    public void notifyEventChanged(Long eventId, String message) {
        List<Registration> regs = regRepo.findByEventIdAndStatusIn(eventId,
                List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED, RegistrationStatus.PENDING));
        for (Registration r : regs) {
            String title = "Cập nhật sự kiện: " + r.getEvent().getTitle();
            notifRepo.save(Notification.of(r.getUser(), title, message));
            try {
                emailSender.send(r.getUser().getEmail(), title, message, null);
            } catch (Exception ex) {
                log.warn("Không gửi được email thay đổi sự kiện tới {}: {}", r.getUser().getEmail(), ex.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long userId, Pageable pageable) {
        return notifRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(n -> new NotificationResponse(n.getId(), n.getTitle(), n.getContent(), n.isRead(), n.getCreatedAt()));
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notifRepo.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Thông báo không tồn tại"));
        if (!n.getUser().getId().equals(userId)) throw new AccessDeniedException("Không phải thông báo của bạn");
        n.setRead(true);
    }
}
