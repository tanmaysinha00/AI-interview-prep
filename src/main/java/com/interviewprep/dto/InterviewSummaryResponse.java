package com.interviewprep.dto;

import java.util.List;
import java.util.UUID;

public record InterviewSummaryResponse(
        UUID interviewId,
        double overallScore,
        List<TopicBreakdown> topicBreakdown,
        List<DifficultyDataPoint> difficultyProgression,
        List<String> strengths,
        List<String> weaknesses,
        String studyPlan
) {
    public record TopicBreakdown(
            String topic,
            double averageScore,
            int questionsAnswered
    ) {}

    public record DifficultyDataPoint(
            int sequenceNumber,
            String difficulty
    ) {}

    /** Internal DTO for the Claude summarization response. Not exposed in the API. */
    public record SummaryClaudePayload(
            List<String> strengths,
            List<String> weaknesses,
            String studyPlan
    ) {}
}
