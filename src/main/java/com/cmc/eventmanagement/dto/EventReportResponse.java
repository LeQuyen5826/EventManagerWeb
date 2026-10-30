package com.cmc.eventmanagement.dto;

public record EventReportResponse(Long eventId, String title, int capacity,
                                  long confirmed, long waitlisted, long cancelled, long checkedIn,
                                  double attendanceRate) {}
