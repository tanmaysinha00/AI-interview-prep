package com.interviewprep.service;

import com.interviewprep.aspect.LogExecutionTime;
import com.interviewprep.dto.EvaluationResponse.DifficultyAdjustment;
import com.interviewprep.entity.DifficultyLevel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calculates adaptive difficulty adjustments based on rolling performance metrics.
 * Evaluates after every 2-3 answered questions.
 */
@Service
public class DifficultyEngine {

    private static final double HIGH_SCORE_THRESHOLD = 8.0;
    private static final double LOW_SCORE_THRESHOLD  = 4.0;
    private static final int    STREAK_INCREASE      = 3;   // 3 high scores → increase
    private static final int    STREAK_DECREASE      = 2;   // 2 low scores → decrease

    @LogExecutionTime
    public DifficultyAdjustment calculateAdjustment(List<Double> recentScores) {
        if (recentScores == null || recentScores.isEmpty()) {
            return DifficultyAdjustment.STAY;
        }

        // Check streak conditions first (take priority over rolling average)
        if (recentScores.size() >= STREAK_INCREASE) {
            List<Double> last3 = recentScores.subList(
                    Math.max(0, recentScores.size() - STREAK_INCREASE), recentScores.size());
            if (last3.stream().allMatch(s -> s >= HIGH_SCORE_THRESHOLD)) {
                return DifficultyAdjustment.INCREASE;
            }
        }

        if (recentScores.size() >= STREAK_DECREASE) {
            List<Double> last2 = recentScores.subList(
                    Math.max(0, recentScores.size() - STREAK_DECREASE), recentScores.size());
            if (last2.stream().allMatch(s -> s <= LOW_SCORE_THRESHOLD)) {
                return DifficultyAdjustment.DECREASE;
            }
        }

        // Rolling average of last 3 scores
        int windowSize = Math.min(3, recentScores.size());
        List<Double> window = recentScores.subList(recentScores.size() - windowSize, recentScores.size());
        double avg = window.stream().mapToDouble(Double::doubleValue).average().orElse(5.0);

        if (avg >= HIGH_SCORE_THRESHOLD) return DifficultyAdjustment.INCREASE;
        if (avg <= LOW_SCORE_THRESHOLD)  return DifficultyAdjustment.DECREASE;
        return DifficultyAdjustment.STAY;
    }

    @LogExecutionTime
    public DifficultyLevel applyAdjustment(DifficultyLevel current, DifficultyAdjustment adjustment) {
        return switch (adjustment) {
            case INCREASE -> current.increase();
            case DECREASE -> current.decrease();
            case STAY     -> current;
        };
    }
}
