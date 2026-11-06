package com.cmc.eventmanagement.service.impl;

import com.cmc.eventmanagement.service.EmailSender;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void send(String to, String subject, String text, byte[] pngAttachment) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(msg, pngAttachment != null, "UTF-8");
            h.setFrom(from);
            h.setTo(to);
            h.setSubject(subject);
            h.setText(text, false);
            if (pngAttachment != null) {
                h.addAttachment("ve-qr.png", new ByteArrayResource(pngAttachment), "image/png");
            }
            mailSender.send(msg);
        } catch (Exception ex) {
            // Gửi lỗi không được làm hỏng nghiệp vụ; ghi log để gửi lại sau (độ tin cậy)
            throw new IllegalStateException("Gửi email thất bại tới " + to, ex);
        }
    }
}
