package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.event.EventChangedEvent;
import com.cmc.eventmanagement.event.RegistrationCancelledEvent;
import com.cmc.eventmanagement.event.RegistrationConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Email chỉ gửi SAU KHI transaction commit và chạy bất đồng bộ: rollback thì không có email nào bị gửi nhầm. */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationEmailListener {
    private final NotificationService notificationService;

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmed(RegistrationConfirmedEvent e) {
        try { notificationService.sendConfirmationEmail(e.registrationId()); }
        catch (Exception ex) { log.error("Gửi email xác nhận thất bại (registration {})", e.registrationId(), ex); }
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelled(RegistrationCancelledEvent e) {
        try { notificationService.sendCancellationEmail(e.registrationId()); }
        catch (Exception ex) { log.error("Gửi email hủy thất bại (registration {})", e.registrationId(), ex); }
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventChanged(EventChangedEvent e) {
        try { notificationService.notifyEventChanged(e.eventId(), e.message()); }
        catch (Exception ex) { log.error("Gửi thông báo thay đổi sự kiện thất bại (event {})", e.eventId(), ex); }
    }
}
