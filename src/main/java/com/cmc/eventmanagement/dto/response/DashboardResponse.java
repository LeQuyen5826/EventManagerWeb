package com.cmc.eventmanagement.dto.response;

import java.util.Map;

/** FR54: dashboard tổng quan cho Admin. */
public record DashboardResponse(long totalEvents, long totalUsers, long totalRegistrations,
                                Map<String, Long> eventsByStatus) {}
