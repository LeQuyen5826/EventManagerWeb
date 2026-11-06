package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.entity.EventStatus;
import com.cmc.eventmanagement.entity.RegistrationStatus;
import com.cmc.eventmanagement.repository.EventRepository;
import com.cmc.eventmanagement.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Tự động chuyển trạng thái sự kiện (Đã công bố -> Đang diễn ra -> Đã kết thúc) và áp dụng BR10. */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventStatusScheduler {
    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void refreshStatuses() {
        LocalDateTime now = LocalDateTime.now();

        // 1) Kết thúc trước (sự kiện đã quá giờ kết thúc, kể cả khi chưa kịp sang ONGOING)
        for (EventStatus st : new EventStatus[]{EventStatus.PUBLISHED, EventStatus.ONGOING}) {
            for (Event e : eventRepo.findByStatusAndEndAtLessThanEqual(st, now)) {
                e.setStatus(EventStatus.ENDED);
                int n = regRepo.bulkChangeStatus(e.getId(), RegistrationStatus.CONFIRMED, RegistrationStatus.NO_SHOW);   // BR10
                log.info("Sự kiện {} kết thúc, {} lượt đăng ký chuyển sang NO_SHOW", e.getId(), n);
            }
        }
        // 2) Bắt đầu
        for (Event e : eventRepo.findByStatusAndStartAtLessThanEqual(EventStatus.PUBLISHED, now)) {
            e.setStatus(EventStatus.ONGOING);
        }
    }
}
