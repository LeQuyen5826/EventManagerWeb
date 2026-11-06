package com.cmc.eventmanagement.dto.response;

import java.time.LocalDate;
import java.util.Map;

/** FR53: báo cáo theo sự kiện. attendanceRate = check-in / đăng ký hợp lệ. */
public record EventReportResponse(Long eventId, String title, int capacity,
                                  long registered, long cancelled, long waitlisted,
                                  long checkedIn, long noShow, double attendanceRate,
                                  Map<LocalDate, Long> registrationsByDay,
                                  Map<Integer, Long> checkInsByHour) {}
