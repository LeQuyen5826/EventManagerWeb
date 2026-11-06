package com.cmc.eventmanagement.entity;

/** Nháp -> (Chờ duyệt) -> Đã công bố -> Đang diễn ra -> Đã kết thúc | Bị từ chối | Đã hủy */
public enum EventStatus { DRAFT, PENDING_APPROVAL, PUBLISHED, REJECTED, ONGOING, ENDED, CANCELLED }
