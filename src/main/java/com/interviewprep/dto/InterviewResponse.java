package com.interviewprep.dto;

import com.interviewprep.entity.InterviewStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InterviewResponse(
        UUID interviewId,
        InterviewStatus status,
        List<String> topics,
        InterviewConfig config,
        BigDecimal totalScore,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {
    public record InterviewConfig(
            int questionCount,
            String initialDifficulty,
            String currentDifficulty
    ) {}
}
