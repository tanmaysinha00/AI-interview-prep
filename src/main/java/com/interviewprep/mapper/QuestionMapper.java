package com.interviewprep.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.dto.QuestionResponse;
import com.interviewprep.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a persisted Question entity back to QuestionResponse DTO.
 * Used when replaying a question that was already generated and saved.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class QuestionMapper {

    private static final Logger log = LoggerFactory.getLogger(QuestionMapper.class);

    @Autowired
    protected ObjectMapper objectMapper;

    @Mapping(target = "questionId", source = "id")
    @Mapping(target = "question", expression = "java(parseDetail(question.getQuestionPayload()))")
    @Mapping(target = "metadata", expression = "java(parseMetadata(question.getQuestionPayload()))")
    public abstract QuestionResponse toResponse(Question question);

    protected QuestionResponse.QuestionDetail parseDetail(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            return new QuestionResponse.QuestionDetail(
                    textOrEmpty(root, "title"),
                    textOrEmpty(root, "body"),
                    textOrNull(root, "codeSnippet"),
                    stringList(root, "hints"),
                    stringListOrNull(root, "options"),
                    root.path("timeEstimateSeconds").asInt(300)
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse question payload: {}", e.getMessage());
            return new QuestionResponse.QuestionDetail(payload, "", null, List.of(), null, 300);
        }
    }

    protected QuestionResponse.QuestionMetadata parseMetadata(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            return new QuestionResponse.QuestionMetadata(
                    stringList(root, "conceptsTested"),
                    stringList(root, "relatedTopics")
            );
        } catch (JsonProcessingException e) {
            return new QuestionResponse.QuestionMetadata(List.of(), List.of());
        }
    }

    private String textOrEmpty(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? "" : n.asText();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return (n.isMissingNode() || n.isNull()) ? null : n.asText();
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
        if (arr.isNull() || arr.isMissingNode() || !arr.isArray()) return null;
        List<String> result = new ArrayList<>();
        arr.forEach(e -> result.add(e.asText()));
        return result.isEmpty() ? null : result;
    }
}
