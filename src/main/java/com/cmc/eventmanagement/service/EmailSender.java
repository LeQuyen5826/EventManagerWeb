package com.cmc.eventmanagement.service;

/** NotificationService chỉ biết interface này; môi trường chạy chọn implementation bằng @Profile. */
public interface EmailSender {
    void send(String to, String subject, String text, byte[] pngAttachment);
}
