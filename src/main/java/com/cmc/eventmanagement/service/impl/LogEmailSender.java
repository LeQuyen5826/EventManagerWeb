package com.cmc.eventmanagement.service.impl;

import com.cmc.eventmanagement.service.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
@Slf4j
public class LogEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String text, byte[] pngAttachment) {
        log.info("[EMAIL giả lập] to={} | subject={} | attachment={} byte\n{}",
                to, subject, pngAttachment == null ? 0 : pngAttachment.length, text);
    }
}
