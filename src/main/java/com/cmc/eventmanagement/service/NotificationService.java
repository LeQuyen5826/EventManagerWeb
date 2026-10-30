package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.entity.Registration;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final EmailSender emailSender;
    private final RegistrationRepository regRepo;

    @Transactional(readOnly = true)
    public void sendCancellation(Long registrationId) {
        Registration r = load(registrationId);
        emailSender.send(r.getUser().getEmail(),
                "Xác nhận hủy đăng ký: " + r.getEvent().getTitle(),
                "Xin chào " + r.getUser().getFullName() + ", đăng ký của bạn cho sự kiện \""
                        + r.getEvent().getTitle() + "\" đã được hủy. Mã QR cũ không còn hiệu lực.");
    }

    @Transactional(readOnly = true)
    public void sendConfirmation(Long registrationId) {
        Registration r = load(registrationId);
        emailSender.send(r.getUser().getEmail(),
                "Đăng ký thành công: " + r.getEvent().getTitle(),
                "Xin chào " + r.getUser().getFullName() + ", bạn đã đăng ký thành công sự kiện \""
                        + r.getEvent().getTitle() + "\". Mã vé của bạn: " + r.getQrToken());
    }

    private Registration load(Long id) {
        return regRepo.findById(id).orElseThrow(() -> new NotFoundException("Đăng ký không tồn tại"));
    }
}
