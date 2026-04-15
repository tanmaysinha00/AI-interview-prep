package com.interviewprep.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaudeApiResponse(
        List<ClaudeContent> content,
        ClaudeUsage usage
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClaudeContent(
            String type,
            String text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClaudeUsage(
            @JsonProperty("input_tokens") int inputTokens,
            @JsonProperty("output_tokens") int outputTokens
    ) {}

    /** Returns the text from the first content block, or empty string if none. */
    public String firstText() {
        if (content == null || content.isEmpty()) return "";
        return content.stream()
                .filter(c -> "text".equals(c.type()))
                .map(ClaudeContent::text)
                .findFirst()
                .orElse("");
    }
}
