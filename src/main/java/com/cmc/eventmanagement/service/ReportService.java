package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.response.DashboardResponse;
import com.cmc.eventmanagement.dto.response.EventReportResponse;
import com.cmc.eventmanagement.entity.Event;
import com.cmc.eventmanagement.entity.Registration;
import com.cmc.eventmanagement.entity.RegistrationStatus;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.*;
import com.cmc.eventmanagement.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final EventRepository eventRepo;
    private final RegistrationRepository regRepo;
    private final CheckInRepository checkInRepo;
    private final UserRepository userRepo;
    private final EventStaffGuard guard;

    /** FR53: báo cáo theo sự kiện. Tỷ lệ tham dự = check-in / đăng ký hợp lệ. */
    @Transactional(readOnly = true)
    public EventReportResponse eventReport(Long eventId, AppUserDetails actor) {
        Event e = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        guard.assertCanManage(e, actor);    // BR08: organizer chỉ xem báo cáo sự kiện của mình

        Map<RegistrationStatus, Long> byStatus = new EnumMap<>(RegistrationStatus.class);
        for (Object[] row : regRepo.countByStatus(eventId)) {
            byStatus.put((RegistrationStatus) row[0], (Long) row[1]);
        }
        long checkedIn = byStatus.getOrDefault(RegistrationStatus.CHECKED_IN, 0L);
        long noShow = byStatus.getOrDefault(RegistrationStatus.NO_SHOW, 0L);
        long registered = byStatus.getOrDefault(RegistrationStatus.CONFIRMED, 0L) + checkedIn + noShow;
        double rate = registered == 0 ? 0 : Math.round(checkedIn * 10000.0 / registered) / 100.0;

        Map<LocalDate, Long> byDay = regRepo.findCreatedAtByEventExcluding(eventId, RegistrationStatus.WAITLISTED).stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, TreeMap::new, Collectors.counting()));
        Map<Integer, Long> byHour = checkInRepo.findCheckInTimesByEvent(eventId).stream()
                .collect(Collectors.groupingBy(LocalDateTime::getHour, TreeMap::new, Collectors.counting()));

        return new EventReportResponse(e.getId(), e.getTitle(), e.getCapacity(), registered,
                byStatus.getOrDefault(RegistrationStatus.CANCELLED, 0L),
                byStatus.getOrDefault(RegistrationStatus.WAITLISTED, 0L),
                checkedIn, noShow, rate, byDay, byHour);
    }

    /** FR55: xuất danh sách đăng ký ra Excel. (Xuất PDF: xem README, hướng phát triển.) */
    @Transactional(readOnly = true)
    public byte[] exportRegistrationsExcel(Long eventId, AppUserDetails actor) {
        Event e = eventRepo.findById(eventId).orElseThrow(() -> new NotFoundException("Sự kiện không tồn tại"));
        guard.assertCanManage(e, actor);
        List<Registration> regs = regRepo.findByEventIdAndStatusIn(eventId, List.of(RegistrationStatus.values()));

        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Danh sách đăng ký");
            String[] headers = {"STT", "Họ tên", "Email", "SĐT", "MSSV", "Đơn vị", "Trạng thái", "Thời gian đăng ký"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) head.createCell(i).setCellValue(headers[i]);
            int i = 1;
            for (Registration r : regs) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(i);
                row.createCell(1).setCellValue(r.getUser().getFullName());
                row.createCell(2).setCellValue(r.getUser().getEmail());
                row.createCell(3).setCellValue(Objects.toString(r.getUser().getPhone(), ""));
                row.createCell(4).setCellValue(Objects.toString(r.getStudentId(), ""));
                row.createCell(5).setCellValue(Objects.toString(r.getOrganization(), ""));
                row.createCell(6).setCellValue(r.getStatus().name());
                row.createCell(7).setCellValue(r.getCreatedAt().toString());
                i++;
            }
            for (int c = 0; c < headers.length; c++) sheet.autoSizeColumn(c);
            wb.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Không xuất được file Excel", ex);
        }
    }

    /** FR54: dashboard tổng quan cho Admin. */
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (var st : com.cmc.eventmanagement.entity.EventStatus.values()) {
            byStatus.put(st.name(), eventRepo.countByStatus(st));
        }
        return new DashboardResponse(eventRepo.count(), userRepo.count(), regRepo.count(), byStatus);
    }
}
