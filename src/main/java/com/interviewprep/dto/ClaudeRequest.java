package com.interviewprep.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<ClaudeMessage> messages
) {
    public record ClaudeMessage(
            String role,
            String content
    ) {
        public static ClaudeMessage user(String content) {
            return new ClaudeMessage("user", content);
        }
    }
}
