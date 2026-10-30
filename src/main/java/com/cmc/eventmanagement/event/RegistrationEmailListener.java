package com.cmc.eventmanagement.event;

import com.cmc.eventmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Email chỉ gửi SAU KHI transaction commit; transaction bị hủy thì không có email nào gửi nhầm. */
@Component
@RequiredArgsConstructor
public class RegistrationEmailListener {
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCancelled(RegistrationCancelledEvent e) {
        notificationService.sendCancellation(e.registrationId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onConfirmed(RegistrationConfirmedEvent e) {
        notificationService.sendConfirmation(e.registrationId());
    }
}
