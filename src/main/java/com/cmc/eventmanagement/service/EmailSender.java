package com.cmc.eventmanagement.service;

/** NotificationService chỉ phụ thuộc interface này (giảm coupling). */
public interface EmailSender {
    void send(String to, String subject, String body);
}
