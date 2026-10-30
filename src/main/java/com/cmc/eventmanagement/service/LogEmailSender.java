package com.cmc.eventmanagement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Môi trường phát triển: chỉ ghi log thay vì gửi mail thật. Mặc định đang dùng bean này. */
@Component
@Profile("!smtp")
@Slf4j
public class LogEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String body) {
        log.info("[EMAIL giả lập] tới={} | tiêu đề={} | nội dung={}", to, subject, body);
    }
}
