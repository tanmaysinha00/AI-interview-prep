package com.interviewprep.dto;

import java.util.List;

public record PlatformMetricsResponse(
        // User counts
        long totalUsers,
        long pendingUsers,
        long activeUsers,
        long suspendedUsers,
        long newUsersLast7Days,
        long newUsersLast30Days,

        // Engagement
        long activeUsersLast7Days,   // at least one interview in last 7 days
        long activeUsersLast30Days,
        long dormantUsers,           // registered but never started an interview
        long powerUsers,             // 3+ completed interviews

        // Interviews & tokens
        long totalInterviews,
        long interviewsLast7Days,
        long totalTokensUsed,

        // Time series (last 30 days)
        List<DayCount> newUsersByDay,
        List<DayCount> interviewsByDay
) {
    public record DayCount(String date, long count) {}
}
