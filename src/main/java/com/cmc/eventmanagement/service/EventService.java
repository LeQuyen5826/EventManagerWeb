package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.request.EventCreateRequest;
import com.cmc.eventmanagement.dto.request.SessionRequest;
import com.cmc.eventmanagement.dto.response.EventResponse;
import com.cmc.eventmanagement.dto.response.EventSummaryResponse;
import com.cmc.eventmanagement.dto.response.SessionResponse;
import com.cmc.eventmanagement.entity.*;
import com.cmc.eventmanagement.event.EventChangedEvent;
import com.cmc.eventmanagement.exception.BusinessException;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.mapper.EventMapper;
import com.cmc.eventmanagement.repository.*;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final List<RegistrationStatus> SEAT_STATUSES =
            List.of(RegistrationStatus.CONFIRMED, RegistrationStatus.CHECKED_IN);
    private static final Set<EventStatus> PUBLIC_STATUSES =
            EnumSet.of(EventStatus.PUBLISHED, EventStatus.ONGOING, EventStatus.ENDED, EventStatus.CANCELLED);

    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final UserRepository userRepo;
    private final EventCategoryRepository categoryRepo;
    private final VenueRepository venueRepo;
    private final EventMapper mapper;
    private final EventStaffGuard guard;
    private final AuditLogService auditLog;
    private final ApplicationEventPublisher publisher;

    @Value("${app.event.require-approval:false}")
    private boolean requireApproval;

    // ---------------------------------------------------------------- CRUD

    /** FR09: lưu ở trạng thái DRAFT. */
    @Transactional
    public EventResponse create(EventCreateRequest req, AppUserDetails actor) {
        EventCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Loại sự kiện không tồn tại"));
        Venue venue = venueRepo.findById(req.venueId())
                .orElseThrow(() -> new NotFoundException("Địa điểm không tồn tại"));
        validateRules(req, venue);

        Event e = new Event();
        e.setOrganizer(userRepo.findById(actor.getId()).orElseThrow());
        e.setCategory(category);
        e.setVenue(venue);
        apply(e, req);
        e.setStatus(EventStatus.DRAFT);
        e = eventRepo.save(e);
        auditLog.log(actor.getId(), "EVENT_CREATE", "eventId=" + e.getId());
        return mapper.toResponse(e, 0);
    }

    /** FR10: sửa thông tin; không giảm sức chứa dưới số đã đăng ký; tự thông báo nếu đổi giờ/địa điểm. */
    @Transactional
    public EventResponse update(Long id, EventCreateRequest req, AppUserDetails actor) {
        Event e = load(id);
        guard.assertCanManage(e, actor);
        if (e.getStatus() == EventStatus.CANCELLED || e.getStatus() == EventStatus.ENDED) {
            throw new BusinessException("Không thể sửa sự kiện đã kết thúc hoặc đã hủy");
        }
        EventCategory category = categoryRepo.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Loại sự kiện không tồn tại"));
        Venue venue = venueRepo.findById(req.venueId())
                .orElseThrow(() -> new NotFoundException("Địa điểm không tồn tại"));
        validateRules(req, venue);

        long taken = regRepo.countSeatsTaken(id);
        if (req.capacity() < taken) {
            throw new BusinessException("Sức chứa không được thấp hơn số người đã đăng ký (" + taken + ")");
        }
        boolean changedWhenOrWhere = !e.getStartAt().equals(req.startAt()) || !e.getEndAt().equals(req.endAt())
                || !e.getVenue().getId().equals(venue.getId());

        e.setCategory(category);
        e.setVenue(venue);
        apply(e, req);

        if (changedWhenOrWhere && (e.getStatus() == EventStatus.PUBLISHED || e.getStatus() == EventStatus.ONGOING)) {
            publisher.publishEvent(new EventChangedEvent(e.getId(),
                    "Sự kiện \"" + e.getTitle() + "\" đã thay đổi thời gian/địa điểm. Vui lòng xem lại thông tin mới."));
        }
        auditLog.log(actor.getId(), "EVENT_UPDATE", "eventId=" + id);
        return mapper.toResponse(e, taken);
    }

    /** FR11: Nháp -> Chờ duyệt (nếu bật duyệt) hoặc Đã công bố. */
    @Transactional
    public EventResponse publish(Long id, AppUserDetails actor) {
        Event e = load(id);
        guard.assertCanManage(e, actor);
        if (e.getStatus() != EventStatus.DRAFT && e.getStatus() != EventStatus.REJECTED) {
            throw new BusinessException("Chỉ công bố được sự kiện ở trạng thái Nháp hoặc Bị từ chối");
        }
        e.setStatus(requireApproval ? EventStatus.PENDING_APPROVAL : EventStatus.PUBLISHED);
        e.setRejectReason(null);
        auditLog.log(actor.getId(), "EVENT_PUBLISH", "eventId=" + id + ", status=" + e.getStatus());
        return mapper.toResponse(e, regRepo.countSeatsTaken(id));
    }

    /** FR12: Admin duyệt. */
    @Transactional
    public EventResponse approve(Long id, AppUserDetails actor) {
        Event e = load(id);
        if (e.getStatus() != EventStatus.PENDING_APPROVAL) throw new BusinessException("Sự kiện không ở trạng thái chờ duyệt");
        e.setStatus(EventStatus.PUBLISHED);
        auditLog.log(actor.getId(), "EVENT_APPROVE", "eventId=" + id);
        return mapper.toResponse(e, regRepo.countSeatsTaken(id));
    }

    /** FR12: Admin từ chối, ghi lý do. */
    @Transactional
    public EventResponse reject(Long id, String reason, AppUserDetails actor) {
        Event e = load(id);
        if (e.getStatus() != EventStatus.PENDING_APPROVAL) throw new BusinessException("Sự kiện không ở trạng thái chờ duyệt");
        e.setStatus(EventStatus.REJECTED);
        e.setRejectReason(reason);
        auditLog.log(actor.getId(), "EVENT_REJECT", "eventId=" + id + ", reason=" + reason);
        return mapper.toResponse(e, 0);
    }

    /** FR13: hủy kèm lý do, hủy mọi đăng ký còn hiệu lực và thông báo cho người đã đăng ký. */
    @Transactional
    public EventResponse cancel(Long id, String reason, AppUserDetails actor) {
        Event e = eventRepo.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        guard.assertCanManage(e, actor);
        if (e.getStatus() == EventStatus.CANCELLED || e.getStatus() == EventStatus.ENDED) {
            throw new BusinessException("Sự kiện đã kết thúc hoặc đã bị hủy");
        }
        boolean wasPublic = e.getStatus() == EventStatus.PUBLISHED || e.getStatus() == EventStatus.ONGOING;
        e.setStatus(EventStatus.CANCELLED);
        e.setCancelReason(reason);
        if (wasPublic) {
            // Không đổi trạng thái từng lượt đăng ký: người đăng ký cần còn nằm trong danh sách để nhận thông báo (FR48).
            // Mã QR vẫn bị vô hiệu vì CheckInService từ chối mọi lượt quét của sự kiện đã hủy (FR39).
            publisher.publishEvent(new EventChangedEvent(e.getId(),
                    "Sự kiện \"" + e.getTitle() + "\" đã bị hủy. Lý do: " + reason));
        }
        auditLog.log(actor.getId(), "EVENT_CANCEL", "eventId=" + id + ", reason=" + reason);
        return mapper.toResponse(e, 0);
    }

    /** BR09: chỉ xóa cứng khi chưa có ai đăng ký. */
    @Transactional
    public void delete(Long id, AppUserDetails actor) {
        Event e = load(id);
        guard.assertCanManage(e, actor);
        if (regRepo.existsByEventId(id)) {
            throw new ConflictException("Sự kiện đã có người đăng ký, chỉ có thể hủy, không thể xóa");
        }
        eventRepo.delete(e);
        auditLog.log(actor.getId(), "EVENT_DELETE", "eventId=" + id);
    }

    // ---------------------------------------------------------------- query

    /** FR26: sự kiện chưa công bố chỉ chủ sự kiện / Admin / nhân viên được phân công mới xem được. */
    @Transactional(readOnly = true)
    public EventResponse getById(Long id, AppUserDetails actor) {
        Event e = load(id);
        if (!PUBLIC_STATUSES.contains(e.getStatus())) {
            if (actor == null) throw new NotFoundException("Sự kiện không tồn tại");
            guard.assertCanView(e, actor);
        }
        return mapper.toResponse(e, regRepo.countSeatsTaken(id));
    }

    /** FR23-FR25: danh sách sự kiện công khai (mặc định PUBLISHED). */
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> search(String keyword, Long categoryId, Long venueId,
                                             LocalDateTime from, LocalDateTime to,
                                             EventStatus status, Pageable pageable) {
        EventStatus st = (status == null) ? EventStatus.PUBLISHED : status;
        if (!PUBLIC_STATUSES.contains(st)) st = EventStatus.PUBLISHED;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return toSummaries(eventRepo.search(kw, categoryId, venueId, from, to, st, pageable));
    }

    /** Sự kiện do tôi tạo (mọi trạng thái). */
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> myEvents(AppUserDetails actor, Pageable pageable) {
        return toSummaries(eventRepo.findByOrganizerId(actor.getId(), pageable));
    }

    /** Admin: lọc theo trạng thái (ví dụ PENDING_APPROVAL để duyệt). */
    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> byStatus(EventStatus status, Pageable pageable) {
        return toSummaries(eventRepo.findByStatus(status, pageable));
    }

    private Page<EventSummaryResponse> toSummaries(Page<Event> page) {
        List<Long> ids = page.getContent().stream().map(Event::getId).toList();
        Map<Long, Long> taken = ids.isEmpty() ? Map.of() :
                regRepo.countSeatsTakenByEvents(ids, SEAT_STATUSES).stream()
                        .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return page.map(e -> mapper.toSummary(e, taken.getOrDefault(e.getId(), 0L)));
    }

    // ---------------------------------------------------------------- lịch trình (FR18-FR20)

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(Long eventId) {
        return load(eventId).getSessions().stream()
                .sorted(Comparator.comparing(Session::getStartAt)).map(mapper::toSession).toList();
    }

    @Transactional
    public SessionResponse addSession(Long eventId, SessionRequest req, AppUserDetails actor) {
        Event e = load(eventId);
        guard.assertCanManage(e, actor);
        validateSession(e, req, null);
        Session s = new Session();
        s.setEvent(e);
        fill(s, req);
        e.getSessions().add(s);
        eventRepo.flush();      // để s có id trước khi trả về
        return mapper.toSession(s);
    }

    @Transactional
    public SessionResponse updateSession(Long eventId, Long sessionId, SessionRequest req, AppUserDetails actor) {
        Event e = load(eventId);
        guard.assertCanManage(e, actor);
        Session s = findSession(e, sessionId);
        validateSession(e, req, sessionId);
        fill(s, req);
        return mapper.toSession(s);
    }

    @Transactional
    public void deleteSession(Long eventId, Long sessionId, AppUserDetails actor) {
        Event e = load(eventId);
        guard.assertCanManage(e, actor);
        e.getSessions().remove(findSession(e, sessionId));   // orphanRemoval = true
    }

    // ---------------------------------------------------------------- nhân viên (FR16)

    @Transactional
    public void assignStaff(Long eventId, Long userId, AppUserDetails actor) {
        Event e = load(eventId);
        guard.assertCanManage(e, actor);
        User u = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Người dùng không tồn tại"));
        if (!u.hasRole(RoleName.STAFF)) throw new BusinessException("Người dùng chưa có vai trò Nhân viên (STAFF)");
        e.getStaff().add(u);
        auditLog.log(actor.getId(), "STAFF_ASSIGN", "eventId=" + eventId + ", userId=" + userId);
    }

    @Transactional
    public void removeStaff(Long eventId, Long userId, AppUserDetails actor) {
        Event e = load(eventId);
        guard.assertCanManage(e, actor);
        e.getStaff().removeIf(u -> u.getId().equals(userId));
        auditLog.log(actor.getId(), "STAFF_REMOVE", "eventId=" + eventId + ", userId=" + userId);
    }

    // ---------------------------------------------------------------- helper

    private Event load(Long id) {
        return eventRepo.findById(id).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
    }

    private void apply(Event e, EventCreateRequest r) {
        e.setTitle(r.title());
        e.setDescription(r.description());
        e.setCoverImageUrl(r.coverImageUrl());
        e.setCapacity(r.capacity());
        e.setStartAt(r.startAt());
        e.setEndAt(r.endAt());
        e.setRegistrationOpenAt(r.registrationOpenAt());
        e.setRegistrationCloseAt(r.registrationCloseAt());
    }

    /** BR01, BR02: quy tắc liên trường / cần dữ liệu CSDL nên kiểm tra ở Service. */
    private void validateRules(EventCreateRequest r, Venue venue) {
        if (!r.endAt().isAfter(r.startAt()))
            throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu");
        if (r.registrationCloseAt().isAfter(r.startAt()))
            throw new BusinessException("Hạn đóng đăng ký không được muộn hơn thời gian bắt đầu sự kiện");
        if (!r.registrationCloseAt().isAfter(r.registrationOpenAt()))
            throw new BusinessException("Hạn đóng đăng ký phải sau thời gian mở đăng ký");
        if (r.capacity() > venue.getMaxCapacity())
            throw new BusinessException("Sức chứa vượt quá sức chứa tối đa của địa điểm (" + venue.getMaxCapacity() + ")");
    }

    private void validateSession(Event e, SessionRequest r, Long ignoreId) {
        if (!r.endAt().isAfter(r.startAt()))
            throw new BusinessException("Thời gian kết thúc phiên phải sau thời gian bắt đầu");
        if (r.startAt().isBefore(e.getStartAt()) || r.endAt().isAfter(e.getEndAt()))
            throw new BusinessException("Thời gian phiên phải nằm trong thời gian của sự kiện");   // FR18
        if (r.room() != null && !r.room().isBlank()) {                                              // FR19
            boolean overlap = e.getSessions().stream()
                    .filter(s -> ignoreId == null || !s.getId().equals(ignoreId))
                    .filter(s -> r.room().equalsIgnoreCase(s.getRoom()))
                    .anyMatch(s -> r.startAt().isBefore(s.getEndAt()) && r.endAt().isAfter(s.getStartAt()));
            if (overlap) throw new ConflictException("Phòng \"" + r.room() + "\" đã có phiên khác trong khung giờ này");
        }
    }

    private void fill(Session s, SessionRequest r) {
        s.setTitle(r.title());
        s.setStartAt(r.startAt());
        s.setEndAt(r.endAt());
        s.setRoom(r.room());
        s.setSpeakerName(r.speakerName());
        s.setDescription(r.description());
    }

    private Session findSession(Event e, Long sessionId) {
        return e.getSessions().stream().filter(s -> s.getId().equals(sessionId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Phiên không tồn tại"));
    }
}
