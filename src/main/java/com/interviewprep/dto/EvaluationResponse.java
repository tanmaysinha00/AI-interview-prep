package com.interviewprep.dto;

import java.util.List;
import java.util.UUID;

public record EvaluationResponse(
        UUID questionId,
        double score,
        int maxScore,
        Verdict verdict,
        EvaluationFeedback feedback,
        DifficultyAdjustment difficultyAdjustment
) {
    public enum Verdict {
        CORRECT, PARTIALLY_CORRECT, INCORRECT
    }

    public enum DifficultyAdjustment {
        STAY, INCREASE, DECREASE
    }

    public record EvaluationFeedback(
            String summary,
            String detailed,
            String correctApproach,
            List<String> commonMistakes,
            String followUpSuggestion
    ) {}
}
