package com.cmc.eventmanagement.event;

/** FR48: sự kiện đổi thời gian/địa điểm hoặc bị hủy -> báo cho người đã đăng ký. */
public record EventChangedEvent(Long eventId, String message) {}
