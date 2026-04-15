package com.interviewprep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.aspect.LogExecutionTime;
import com.interviewprep.dto.ClaudeApiResponse;
import com.interviewprep.dto.EvaluationResponse;
import com.interviewprep.dto.QuestionResponse;
import com.interviewprep.entity.Answer;
import com.interviewprep.entity.Question;
import com.interviewprep.entity.TokenUsage;
import com.interviewprep.exception.ClaudeApiException;
import com.interviewprep.repository.AnswerRepository;
import com.interviewprep.repository.TokenUsageRepository;
import com.interviewprep.util.PromptTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class EvaluationService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

    private final ClaudeApiClient claudeApiClient;
    private final ClaudeResponseParser responseParser;
    private final AnswerRepository answerRepository;
    private final TokenUsageRepository tokenUsageRepository;
    private final PromptTemplateBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final String model;

    public EvaluationService(
            ClaudeApiClient claudeApiClient,
            ClaudeResponseParser responseParser,
            AnswerRepository answerRepository,
            TokenUsageRepository tokenUsageRepository,
            PromptTemplateBuilder promptBuilder,
            ObjectMapper objectMapper,
            @Value("${app.claude.model}") String model
    ) {
        this.claudeApiClient = claudeApiClient;
        this.responseParser = responseParser;
        this.answerRepository = answerRepository;
        this.tokenUsageRepository = tokenUsageRepository;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.model = model;
    }

    @LogExecutionTime
    @Transactional
    public EvaluationResponse evaluate(Question question, String userAnswerText, Integer timeTakenSeconds) {
        // Parse question payload to extract title/body/codeSnippet for the prompt
        String title = "";
        String body = "";
        String codeSnippet = null;
        java.util.List<String> options = new java.util.ArrayList<>();
        try {
            var node = objectMapper.readTree(question.getQuestionPayload());
            title = node.path("title").asText();
            body = node.path("body").asText();
            var cs = node.path("codeSnippet");
            if (!cs.isNull() && !cs.isMissingNode()) {
                codeSnippet = cs.asText();
            }
            var optionsNode = node.path("options");
            if (optionsNode.isArray()) {
                for (var opt : optionsNode) {
                    options.add(opt.asText());
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse question payload for evaluation: {}", e.getMessage());
            body = question.getQuestionPayload();
        }

        String systemPrompt = promptBuilder.buildEvaluationSystemPrompt();
        String userPrompt = promptBuilder.buildEvaluationPrompt(
                title, body, codeSnippet, question.getDifficulty(),
                question.getType(), options, userAnswerText);

        ClaudeApiResponse claudeResponse = callWithRetry(systemPrompt, userPrompt);
        String responseJson = claudeResponse.firstText();

        EvaluationResponse evaluation;
        try {
            evaluation = responseParser.parseEvaluationResponse(responseJson, question.getId());
        } catch (ClaudeApiException ex) {
            // Retry once with stricter prompt
            log.warn("Evaluation parse failed, retrying with stricter prompt");
            ClaudeApiResponse retryResponse = claudeApiClient.sendMessage(
                    systemPrompt + "\n\nIMPORTANT: Your previous response was not valid JSON. Respond with ONLY the JSON object, nothing else.",
                    userPrompt);
            evaluation = responseParser.parseEvaluationResponse(retryResponse.firstText(), question.getId());
        }

        // Persist answer
        Answer answer = new Answer();
        answer.setQuestionId(question.getId());
        answer.setUserAnswer(userAnswerText);
        answer.setTimeTakenSeconds(timeTakenSeconds);
        answer.setScore(BigDecimal.valueOf(evaluation.score()));
        try {
            answer.setEvaluationPayload(objectMapper.writeValueAsString(evaluation));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize evaluation payload: {}", e.getMessage());
        }
        answerRepository.save(answer);

        // Track token usage
        recordTokenUsage(question.getInterviewId(), claudeResponse);

        return evaluation;
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private ClaudeApiResponse callWithRetry(String systemPrompt, String userPrompt) {
        return claudeApiClient.sendMessage(systemPrompt, userPrompt);
    }

    private void recordTokenUsage(java.util.UUID interviewId, ClaudeApiResponse response) {
        if (response.usage() == null) return;
        TokenUsage usage = new TokenUsage();
        usage.setInterviewId(interviewId);
        usage.setInputTokens(response.usage().inputTokens());
        usage.setOutputTokens(response.usage().outputTokens());
        usage.setModel(model);
        tokenUsageRepository.save(usage);
    }
}
