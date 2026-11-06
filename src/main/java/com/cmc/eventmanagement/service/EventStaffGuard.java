package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.security.AppUserDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** BR07, BR08: kiểm tra quyền THEO DỮ LIỆU (ai là chủ/nhân viên của sự kiện này). */
@Component
public class EventStaffGuard {

    /** Chỉ chủ sự kiện hoặc nhân viên được phân công mới check-in được (BR07). */
    public void assertCanCheckIn(Event event, AppUserDetails actor) {
        if (event.isOwnedBy(actor.getId()) || event.hasStaff(actor.getId())) return;
        throw new AccessDeniedException("Bạn không được phân công cho sự kiện này");
    }

    /** Xem danh sách đăng ký / số liệu: chủ sự kiện, nhân viên được phân công hoặc Admin. */
    public void assertCanView(Event event, AppUserDetails actor) {
        if (actor.isAdmin() || event.isOwnedBy(actor.getId()) || event.hasStaff(actor.getId())) return;
        throw new AccessDeniedException("Bạn không có quyền xem sự kiện này");
    }

    /** Chỉ chủ sự kiện hoặc Admin được sửa/hủy/quản lý (BR08). */
    public void assertCanManage(Event event, AppUserDetails actor) {
        if (actor.isAdmin() || event.isOwnedBy(actor.getId())) return;
        throw new AccessDeniedException("Bạn không phải chủ sự kiện này");
    }
}
