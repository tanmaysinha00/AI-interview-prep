package com.interviewprep.dto;

import com.interviewprep.entity.DifficultyLevel;
import com.interviewprep.entity.QuestionType;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID questionId,
        UUID interviewId,
        int sequenceNumber,
        String topic,
        DifficultyLevel difficulty,
        QuestionType type,
        QuestionDetail question,
        QuestionMetadata metadata
) {
    public record QuestionDetail(
            String title,
            String body,
            String codeSnippet,
            List<String> hints,
            List<String> options,
            int timeEstimateSeconds
    ) {}

    public record QuestionMetadata(
            List<String> conceptsTested,
            List<String> relatedTopics
    ) {}
}
