package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.*;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.*;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepo;
    private final VenueRepository venueRepo;
    private final EventCategoryRepository categoryRepo;
    private final UserRepository userRepo;
    private final RegistrationRepository regRepo;
    private final AuditLogService auditLog;

    /** FR09: tạo sự kiện ở trạng thái DRAFT. Kiểm tra BR01, BR02. */
    @Transactional
    public EventResponse create(EventCreateRequest r, AppUserDetails me) {
        Venue venue = venueRepo.findById(r.venueId())
                .orElseThrow(() -> new NotFoundException("Địa điểm không tồn tại"));
        EventCategory category = categoryRepo.findById(r.categoryId())
                .orElseThrow(() -> new NotFoundException("Loại sự kiện không tồn tại"));

        // BR01
        if (!r.endAt().isAfter(r.startAt()))
            throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu");
        if (r.registrationCloseAt().isAfter(r.startAt()))
            throw new BusinessException("Hạn đóng đăng ký không được muộn hơn giờ bắt đầu");
        if (!r.registrationCloseAt().isAfter(r.registrationOpenAt()))
            throw new BusinessException("Hạn đóng đăng ký phải sau thời điểm mở đăng ký");
        // BR02
        if (r.capacity() > venue.getMaxCapacity())
            throw new BusinessException("Sức chứa vượt quá sức chứa tối đa của địa điểm (" + venue.getMaxCapacity() + ")");

        Event e = new Event();
        e.setTitle(r.title());
        e.setDescription(r.description());
        e.setCategory(category);
        e.setVenue(venue);
        e.setOrganizer(userRepo.getReferenceById(me.getId()));
        e.setCapacity(r.capacity());
        e.setStartAt(r.startAt());
        e.setEndAt(r.endAt());
        e.setRegistrationOpenAt(r.registrationOpenAt());
        e.setRegistrationCloseAt(r.registrationCloseAt());
        e.setStatus(EventStatus.DRAFT);
        e = eventRepo.save(e);
        auditLog.log(me.getId(), "CREATE_EVENT", "eventId=" + e.getId());
        return EventResponse.from(e);
    }

    /** FR23-FR25: danh sách công khai (chỉ sự kiện PUBLISHED), tìm kiếm, lọc, phân trang. */
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> search(String keyword, Long categoryId, Pageable pageable) {
        String kw = keyword == null ? "" : keyword.trim();
        return eventRepo.search(kw, categoryId, EventStatus.PUBLISHED, pageable).map(EventSummaryResponse::from);
    }

    /** FR26: chi tiết. Bản nháp chỉ chủ sự kiện/ADMIN mới thấy; người khác nhận 404. */
    @Transactional(readOnly = true)
    public EventResponse getById(Long id, AppUserDetails me) {
        Event e = find(id);
        if (e.getStatus() == EventStatus.DRAFT && !canManage(e, me)) {
            throw new NotFoundException("Sự kiện không tồn tại");
        }
        return EventResponse.from(e);
    }

    /** Công bố sự kiện: DRAFT -> PUBLISHED. */
    @Transactional
    public EventResponse publish(Long id, AppUserDetails me) {
        Event e = find(id);
        assertCanManage(e, me);
        if (e.getStatus() != EventStatus.DRAFT)
            throw new BusinessException("Chỉ có thể công bố sự kiện đang ở trạng thái nháp");
        e.setStatus(EventStatus.PUBLISHED);
        auditLog.log(me.getId(), "PUBLISH_EVENT", "eventId=" + id);
        return EventResponse.from(e);
    }

    /** FR13: hủy sự kiện (BR09: không xóa cứng). Các đăng ký còn hiệu lực bị hủy theo. */
    @Transactional
    public void cancel(Long id, String reason, AppUserDetails me) {
        Event e = eventRepo.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        assertCanManage(e, me);
        if (e.getStatus() == EventStatus.CANCELLED) throw new ConflictException("Sự kiện đã bị hủy trước đó");
        e.setStatus(EventStatus.CANCELLED);
        e.setCancelReason(reason);
        regRepo.bulkUpdateStatus(id,
                List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED), RegistrationStatus.CANCELLED);
        auditLog.log(me.getId(), "CANCEL_EVENT", "eventId=" + id + ", reason=" + reason);
    }

    /** Phân công nhân viên check-in cho sự kiện (BR07). */
    @Transactional
    public void assignStaff(Long eventId, Long staffUserId, AppUserDetails me) {
        Event e = find(eventId);
        assertCanManage(e, me);
        User staff = userRepo.findById(staffUserId).orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        boolean isStaff = staff.getRoles().stream().anyMatch(r -> r.getName().equals("STAFF"));
        if (!isStaff) throw new BusinessException("Người dùng được phân công phải có vai trò STAFF");
        e.getStaff().add(staff);
        auditLog.log(me.getId(), "ASSIGN_STAFF", "eventId=" + eventId + ", staffId=" + staffUserId);
    }

    /** FR53: báo cáo sự kiện (BR08: chỉ chủ sự kiện hoặc ADMIN). */
    @Transactional(readOnly = true)
    public EventReportResponse report(Long eventId, AppUserDetails me) {
        Event e = find(eventId);
        assertCanManage(e, me);
        long confirmed = regRepo.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        long waitlisted = regRepo.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
        long cancelled = regRepo.countByEventIdAndStatus(eventId, RegistrationStatus.CANCELLED);
        long checkedIn = regRepo.countByEventIdAndStatus(eventId, RegistrationStatus.CHECKED_IN);
        long active = confirmed + checkedIn;
        double rate = active == 0 ? 0 : Math.round(checkedIn * 1000.0 / active) / 10.0;
        return new EventReportResponse(e.getId(), e.getTitle(), e.getCapacity(),
                confirmed, waitlisted, cancelled, checkedIn, rate);
    }

    // ---- helpers ----
    private Event find(Long id) {
        return eventRepo.findById(id).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
    }

    private boolean canManage(Event e, AppUserDetails me) {
        return me != null && (me.isAdmin() || e.isOrganizedBy(me.getId()));
    }

    /** Phân quyền theo dữ liệu: filter không biết ai là chủ sự kiện nên phải kiểm tra ở Service (403). */
    private void assertCanManage(Event e, AppUserDetails me) {
        if (!canManage(e, me)) throw new AccessDeniedException("Không phải sự kiện của bạn");
    }
}
