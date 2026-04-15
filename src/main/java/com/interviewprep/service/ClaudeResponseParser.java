package com.interviewprep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.aspect.LogExecutionTime;
import com.interviewprep.dto.EvaluationResponse;
import com.interviewprep.dto.InterviewSummaryResponse;
import com.interviewprep.dto.QuestionResponse;
import com.interviewprep.entity.DifficultyLevel;
import com.interviewprep.entity.QuestionType;
import com.interviewprep.exception.ClaudeApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Parses Claude API JSON responses into typed DTOs.
 * On parse failure, throws ClaudeApiException — the caller should retry with a stricter prompt.
 */
@Service
public class ClaudeResponseParser {

    private static final Logger log = LoggerFactory.getLogger(ClaudeResponseParser.class);

    private final ObjectMapper objectMapper;

    public ClaudeResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @LogExecutionTime
    public QuestionResponse parseQuestionResponse(
            String json,
            UUID questionId,
            UUID interviewId,
            int sequenceNumber,
            String topic,
            DifficultyLevel difficulty,
            QuestionType type
    ) {
        try {
            JsonNode root = objectMapper.readTree(sanitize(json));

            QuestionResponse.QuestionDetail detail = new QuestionResponse.QuestionDetail(
                    text(root, "title"),
                    text(root, "body"),
                    textOrNull(root, "codeSnippet"),
                    stringList(root, "hints"),
                    stringListOrNull(root, "options"),
                    intValue(root, "timeEstimateSeconds", 300)
            );

            QuestionResponse.QuestionMetadata metadata = new QuestionResponse.QuestionMetadata(
                    stringList(root, "conceptsTested"),
                    stringList(root, "relatedTopics")
            );

            return new QuestionResponse(questionId, interviewId, sequenceNumber,
                    topic, difficulty, type, detail, metadata);

        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse question JSON: {}", ex.getMessage());
            throw new ClaudeApiException("Failed to parse question response from Claude: " + ex.getMessage(), ex);
        }
    }

    @LogExecutionTime
    public EvaluationResponse parseEvaluationResponse(String json, UUID questionId) {
        try {
            JsonNode root = objectMapper.readTree(sanitize(json));

            JsonNode feedbackNode = root.path("feedback");
            EvaluationResponse.EvaluationFeedback feedback = new EvaluationResponse.EvaluationFeedback(
                    text(feedbackNode, "summary"),
                    text(feedbackNode, "detailed"),
                    text(feedbackNode, "correctApproach"),
                    stringList(feedbackNode, "commonMistakes"),
                    textOrNull(feedbackNode, "followUpSuggestion")
            );

            return new EvaluationResponse(
                    questionId,
                    doubleValue(root, "score", 0.0),
                    intValue(root, "maxScore", 10),
                    parseVerdict(text(root, "verdict")),
                    feedback,
                    parseDifficultyAdjustment(text(root, "difficultyAdjustment"))
            );

        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse evaluation JSON: {}", ex.getMessage());
            throw new ClaudeApiException("Failed to parse evaluation response from Claude: " + ex.getMessage(), ex);
        }
    }

    @LogExecutionTime
    public InterviewSummaryResponse.SummaryClaudePayload parseSummaryResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(sanitize(json));
            return new InterviewSummaryResponse.SummaryClaudePayload(
                    stringList(root, "strengths"),
                    stringList(root, "weaknesses"),
                    text(root, "studyPlan")
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse summary JSON: {}", ex.getMessage());
            throw new ClaudeApiException("Failed to parse summary response from Claude: " + ex.getMessage(), ex);
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    /** Strips markdown fences if Claude ignores the JSON-only instruction. */
    private String sanitize(String json) {
        if (json == null) return "{}";
        String trimmed = json.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return trimmed;
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? "" : n.asText();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? null : n.asText();
    }

    private int intValue(JsonNode node, String field, int defaultValue) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? defaultValue : n.asInt(defaultValue);
    }

    private double doubleValue(JsonNode node, String field, double defaultValue) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? defaultValue : n.asDouble(defaultValue);
    }

    private List<String> stringList(JsonNode node, String field) {
        JsonNode arr = node.path(field);
        if (!arr.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        arr.forEach(e -> result.add(e.asText()));
        return result;
    }

    private List<String> stringListOrNull(JsonNode node, String field) {
        JsonNode arr = node.path(field);
        if (arr.isNull() || arr.isMissingNode()) return null;
        if (!arr.isArray()) return null;
        List<String> result = new ArrayList<>();
        arr.forEach(e -> result.add(e.asText()));
        return result.isEmpty() ? null : result;
    }

    private EvaluationResponse.Verdict parseVerdict(String value) {
        try {
            return EvaluationResponse.Verdict.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EvaluationResponse.Verdict.PARTIALLY_CORRECT;
        }
    }

    private EvaluationResponse.DifficultyAdjustment parseDifficultyAdjustment(String value) {
        try {
            return EvaluationResponse.DifficultyAdjustment.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EvaluationResponse.DifficultyAdjustment.STAY;
        }
    }
}
