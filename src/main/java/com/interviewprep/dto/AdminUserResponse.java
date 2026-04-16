package com.interviewprep.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        String status,
        int loginCount,
        long totalInterviews,
        double avgScore,
        long totalTokensUsed,
        OffsetDateTime createdAt,
        OffsetDateTime lastLogin
) {}
