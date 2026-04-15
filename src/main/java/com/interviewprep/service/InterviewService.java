package com.interviewprep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.aspect.AuditLog;
import com.interviewprep.aspect.LogExecutionTime;
import com.interviewprep.dto.*;
import com.interviewprep.dto.EvaluationResponse.DifficultyAdjustment;
import com.interviewprep.entity.*;
import com.interviewprep.exception.*;
import com.interviewprep.mapper.InterviewMapper;
import com.interviewprep.repository.*;
import com.interviewprep.util.PromptTemplateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private static final List<QuestionType> TYPE_ROTATION = List.of(
            QuestionType.SHORT_ANSWER,
            QuestionType.SCENARIO,
            QuestionType.MCQ,
            QuestionType.CODE_REVIEW,
            QuestionType.SHORT_ANSWER,
            QuestionType.SYSTEM_DESIGN,
            QuestionType.HANDS_ON_CODING,
            QuestionType.SCENARIO,
            QuestionType.SHORT_ANSWER,
            QuestionType.MCQ
    );

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final TokenUsageRepository tokenUsageRepository;
    private final ClaudeApiClient claudeApiClient;
    private final ClaudeResponseParser responseParser;
    private final EvaluationService evaluationService;
    private final DifficultyEngine difficultyEngine;
    private final PromptTemplateBuilder promptBuilder;
    private final InterviewMapper interviewMapper;
    private final ObjectMapper objectMapper;
    private final String model;
    private final long tokenBudget;

    public InterviewService(
            InterviewRepository interviewRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            TokenUsageRepository tokenUsageRepository,
            ClaudeApiClient claudeApiClient,
            ClaudeResponseParser responseParser,
            EvaluationService evaluationService,
            DifficultyEngine difficultyEngine,
            PromptTemplateBuilder promptBuilder,
            InterviewMapper interviewMapper,
            ObjectMapper objectMapper,
            @Value("${app.claude.model}") String model,
            @Value("${app.claude.per-interview-token-budget:50000}") long tokenBudget
    ) {
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.tokenUsageRepository = tokenUsageRepository;
        this.claudeApiClient = claudeApiClient;
        this.responseParser = responseParser;
        this.evaluationService = evaluationService;
        this.difficultyEngine = difficultyEngine;
        this.promptBuilder = promptBuilder;
        this.interviewMapper = interviewMapper;
        this.objectMapper = objectMapper;
        this.model = model;
        this.tokenBudget = tokenBudget;
    }

    // ----------------------------------------------------------------
    // Create interview
    // ----------------------------------------------------------------

    @LogExecutionTime
    @AuditLog(action = "interview.created")
    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request, UUID userId) {
        Interview interview = new Interview();
        interview.setUserId(userId);
        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setTopics(new ArrayList<>(request.topics()));
        interview.setConfig(buildConfigJson(request.questionCount(), request.difficulty(), request.yearsOfExperience()));

        Interview saved = interviewRepository.save(interview);
        log.info("Created interview {} for user {}", saved.getId(), userId);

        return interviewMapper.toResponse(saved);
    }

    // ----------------------------------------------------------------
    // Get next question
    // ----------------------------------------------------------------

    @LogExecutionTime
    @Transactional
    public QuestionResponse getNextQuestion(UUID interviewId, UUID userId) {
        Interview interview = loadInterview(interviewId, userId);

        InterviewConfig config = parseConfig(interview.getConfig());
        int questionCount = config.questionCount();

        // Idempotency: if there's already an unanswered question, return it as-is
        Optional<Question> unanswered = questionRepository.findFirstUnansweredQuestion(interviewId);
        if (unanswered.isPresent()) {
            Question existing = unanswered.get();
            try {
                return responseParser.parseQuestionResponse(
                        existing.getQuestionPayload(), existing.getId(), interviewId,
                        existing.getSequenceNumber(), existing.getTopic(),
                        existing.getDifficulty(), existing.getType());
            } catch (ClaudeApiException ex) {
                log.warn("Failed to parse existing unanswered question {}, will generate new", existing.getId());
            }
        }

        long answeredCount = answerRepository.findByInterviewIdOrderByAnsweredAt(interviewId).size();

        if (answeredCount >= questionCount) {
            // Safety net: mark COMPLETED in case submitAnswer missed it (e.g. data inconsistency)
            if (interview.getStatus() != InterviewStatus.COMPLETED) {
                completeInterview(interview);
            }
            throw new IllegalStateException("All %d questions have been answered for interview %s"
                    .formatted(questionCount, interviewId));
        }

        // Check token budget
        long tokensUsed = tokenUsageRepository.sumTotalTokensByInterviewId(interviewId);
        if (tokensUsed >= tokenBudget) {
            throw new TokenBudgetExceededException(interviewId, tokensUsed, tokenBudget);
        }

        // Pick next topic (round-robin)
        List<String> topics = interview.getTopics();
        String topic = topics.get((int) (answeredCount % topics.size()));

        // Current difficulty from config
        DifficultyLevel difficulty = config.currentDifficulty();

        // Question type from rotation
        QuestionType type = TYPE_ROTATION.get((int) (answeredCount % TYPE_ROTATION.size()));

        // Collect previous topics for context
        List<String> previousTopics = questionRepository.findTopicsByInterviewId(interviewId);

        // Generate via Claude
        String systemPrompt = promptBuilder.buildQuestionSystemPrompt();
        String userPrompt = promptBuilder.buildQuestionPrompt(topic, difficulty, type, previousTopics, config.yearsOfExperience());

        ClaudeApiResponse claudeResponse = claudeApiClient.sendMessage(systemPrompt, userPrompt);
        String responseJson = claudeResponse.firstText();

        // Persist question
        Question question = new Question();
        question.setInterviewId(interviewId);
        question.setSequenceNumber((int) answeredCount + 1);
        question.setTopic(topic);
        question.setDifficulty(difficulty);
        question.setType(type);
        question.setQuestionPayload(responseJson);
        Question saved = questionRepository.save(question);

        // Track token usage
        recordTokenUsage(interviewId, claudeResponse);

        // Check difficulty adjustment every 3 questions
        if (answeredCount > 0 && answeredCount % 3 == 0) {
            recalculateDifficulty(interview);
        }

        // Parse and return
        try {
            return responseParser.parseQuestionResponse(
                    responseJson, saved.getId(), interviewId,
                    saved.getSequenceNumber(), topic, difficulty, type);
        } catch (ClaudeApiException ex) {
            // Retry once
            log.warn("Question parse failed, retrying with stricter prompt for question {}", saved.getId());
            ClaudeApiResponse retry = claudeApiClient.sendMessage(
                    systemPrompt + "\n\nYour previous response was not valid JSON. Respond with ONLY the JSON object.",
                    userPrompt);
            String retryJson = retry.firstText();
            saved.setQuestionPayload(retryJson);
            questionRepository.save(saved);
            return responseParser.parseQuestionResponse(
                    retryJson, saved.getId(), interviewId,
                    saved.getSequenceNumber(), topic, difficulty, type);
        }
    }

    // ----------------------------------------------------------------
    // Submit answer
    // ----------------------------------------------------------------

    @LogExecutionTime
    @Transactional
    public EvaluationResponse submitAnswer(UUID interviewId, UUID questionId,
                                           SubmitAnswerRequest request, UUID userId) {
        Interview interview = loadInterview(interviewId, userId);

        Question question = questionRepository.findByIdAndInterviewId(questionId, interviewId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));

        if (answerRepository.findByQuestionId(questionId).isPresent()) {
            throw new IllegalStateException("Question " + questionId + " has already been answered");
        }

        EvaluationResponse evaluation = evaluationService.evaluate(
                question, request.answer(), request.timeTakenSeconds());

        // answeredCount includes the answer just saved by evaluationService
        long answeredCount = answerRepository.findByInterviewIdOrderByAnsweredAt(interviewId).size();
        if (answeredCount % 3 == 0) {
            recalculateDifficulty(interview);
        }

        // Check if interview is complete
        int questionCount = parseConfig(interview.getConfig()).questionCount();
        if (answeredCount >= questionCount) {
            completeInterview(interview);
        }

        return evaluation;
    }

    // ----------------------------------------------------------------
    // List user's interviews
    // ----------------------------------------------------------------

    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUserInterviews(UUID userId) {
        return interviewRepository.findByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(interviewMapper::toResponse)
                .toList();
    }

    // ----------------------------------------------------------------
    // Interview summary
    // ----------------------------------------------------------------

    @LogExecutionTime
    @AuditLog(action = "interview.summary.requested")
    @Transactional
    public InterviewSummaryResponse getInterviewSummary(UUID interviewId, UUID userId) {
        Interview interview = loadInterview(interviewId, userId);

        // Return cached summary if already generated — avoids redundant Claude API calls
        if (interview.getSummaryPayload() != null) {
            try {
                return objectMapper.readValue(interview.getSummaryPayload(), InterviewSummaryResponse.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached summary for interview {}, regenerating", interviewId);
            }
        }

        List<Question> questions = questionRepository.findByInterviewIdOrderBySequenceNumber(interviewId);
        List<Answer> answers = answerRepository.findByInterviewIdOrderByAnsweredAt(interviewId);

        // Build answer map
        Map<UUID, Answer> answerMap = answers.stream()
                .collect(Collectors.toMap(Answer::getQuestionId, a -> a));

        // Overall score
        double overallScore = answers.stream()
                .filter(a -> a.getScore() != null)
                .mapToDouble(a -> a.getScore().doubleValue())
                .average()
                .orElse(0.0);

        // Topic breakdown
        Map<String, List<Double>> topicScores = new LinkedHashMap<>();
        for (Question q : questions) {
            Answer a = answerMap.get(q.getId());
            if (a != null && a.getScore() != null) {
                topicScores.computeIfAbsent(q.getTopic(), k -> new ArrayList<>())
                           .add(a.getScore().doubleValue());
            }
        }
        List<InterviewSummaryResponse.TopicBreakdown> topicBreakdown = topicScores.entrySet().stream()
                .map(e -> new InterviewSummaryResponse.TopicBreakdown(
                        e.getKey(),
                        e.getValue().stream().mapToDouble(d -> d).average().orElse(0.0),
                        e.getValue().size()))
                .toList();

        // Difficulty progression
        List<InterviewSummaryResponse.DifficultyDataPoint> difficultyProgression = questions.stream()
                .map(q -> new InterviewSummaryResponse.DifficultyDataPoint(
                        q.getSequenceNumber(), q.getDifficulty().name()))
                .toList();

        // Build question summaries for Claude summary call
        List<String> questionSummaries = new ArrayList<>();
        for (Question q : questions) {
            Answer a = answerMap.get(q.getId());
            String summary = "Q%d [%s/%s]: %s — Score: %s/10".formatted(
                    q.getSequenceNumber(), q.getTopic(), q.getDifficulty().name(),
                    extractTitle(q.getQuestionPayload()),
                    a != null && a.getScore() != null ? a.getScore().toPlainString() : "N/A");
            questionSummaries.add(summary);
        }

        // Generate strengths/weaknesses/studyPlan via Claude
        String systemPrompt = promptBuilder.buildSummarySystemPrompt();
        String userPrompt = promptBuilder.buildSummaryPrompt(
                interview.getTopics(), overallScore, questionSummaries);

        ClaudeApiResponse claudeResponse = claudeApiClient.sendMessage(systemPrompt, userPrompt);
        InterviewSummaryResponse.SummaryClaudePayload payload =
                responseParser.parseSummaryResponse(claudeResponse.firstText());

        InterviewSummaryResponse summaryResponse = new InterviewSummaryResponse(
                interviewId,
                Math.round(overallScore * 10.0) / 10.0,
                topicBreakdown,
                difficultyProgression,
                payload.strengths(),
                payload.weaknesses(),
                payload.studyPlan()
        );

        // Persist for future requests — zero Claude cost on re-visits
        try {
            interview.setSummaryPayload(objectMapper.writeValueAsString(summaryResponse));
            interviewRepository.save(interview);
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache summary for interview {}: {}", interviewId, e.getMessage());
        }

        return summaryResponse;
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private Interview loadInterview(UUID interviewId, UUID userId) {
        return interviewRepository.findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new InterviewNotFoundException(interviewId));
    }

    private void recalculateDifficulty(Interview interview) {
        List<Answer> answers = answerRepository.findByInterviewIdOrderByAnsweredAt(interview.getId());
        List<Double> scores = answers.stream()
                .filter(a -> a.getScore() != null)
                .map(a -> a.getScore().doubleValue())
                .toList();

        if (scores.isEmpty()) return;

        InterviewConfig config = parseConfig(interview.getConfig());
        DifficultyAdjustment adjustment = difficultyEngine.calculateAdjustment(scores);
        DifficultyLevel newDifficulty = difficultyEngine.applyAdjustment(config.currentDifficulty(), adjustment);

        if (newDifficulty != config.currentDifficulty()) {
            log.info("Difficulty adjusted for interview {}: {} → {} (adjustment={})",
                    interview.getId(), config.currentDifficulty(), newDifficulty, adjustment);
            interview.setConfig(buildConfigJson(config.questionCount(),
                    config.initialDifficulty(), newDifficulty));
            interviewRepository.save(interview);
        }
    }

    // Note: @AuditLog is not applied here because Spring AOP does not intercept private methods.
    // The completion event is logged explicitly via log.info below.
    private void completeInterview(Interview interview) {
        List<Answer> answers = answerRepository.findByInterviewIdOrderByAnsweredAt(interview.getId());
        double avg = answers.stream()
                .filter(a -> a.getScore() != null)
                .mapToDouble(a -> a.getScore().doubleValue())
                .average()
                .orElse(0.0);

        interview.setStatus(InterviewStatus.COMPLETED);
        interview.setCompletedAt(OffsetDateTime.now());
        interview.setTotalScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        interviewRepository.save(interview);
        log.info("Interview {} completed with score {}", interview.getId(), avg);
    }

    private void recordTokenUsage(UUID interviewId, ClaudeApiResponse response) {
        if (response.usage() == null) return;
        com.interviewprep.entity.TokenUsage usage = new com.interviewprep.entity.TokenUsage();
        usage.setInterviewId(interviewId);
        usage.setInputTokens(response.usage().inputTokens());
        usage.setOutputTokens(response.usage().outputTokens());
        usage.setModel(model);
        tokenUsageRepository.save(usage);
    }

    private String buildConfigJson(int questionCount, DifficultyLevel difficulty, int yearsOfExperience) {
        return buildConfigJson(questionCount, difficulty, difficulty, yearsOfExperience);
    }

    private String buildConfigJson(int questionCount, DifficultyLevel initial, DifficultyLevel current) {
        return buildConfigJson(questionCount, initial, current, 0);
    }

    private String buildConfigJson(int questionCount, DifficultyLevel initial, DifficultyLevel current, int yearsOfExperience) {
        return """
                {"questionCount":%d,"initialDifficulty":"%s","currentDifficulty":"%s","yearsOfExperience":%d}"""
                .formatted(questionCount, initial.name(), current.name(), yearsOfExperience);
    }

    private InterviewConfig parseConfig(String json) {
        try {
            var node = objectMapper.readTree(json);
            int qc = node.path("questionCount").asInt(10);
            String initial = node.path("initialDifficulty").asText("MEDIUM");
            String current = node.path("currentDifficulty").asText(initial);
            int yoe = node.path("yearsOfExperience").asInt(0);
            return new InterviewConfig(qc, DifficultyLevel.valueOf(initial), DifficultyLevel.valueOf(current), yoe);
        } catch (Exception e) {
            return new InterviewConfig(10, DifficultyLevel.MEDIUM, DifficultyLevel.MEDIUM, 0);
        }
    }

    private String extractTitle(String questionPayload) {
        try {
            return objectMapper.readTree(questionPayload).path("title").asText("Question");
        } catch (Exception e) {
            return "Question";
        }
    }

    /** Internal record for parsed config JSON. */
    private record InterviewConfig(int questionCount, DifficultyLevel initialDifficulty, DifficultyLevel currentDifficulty, int yearsOfExperience) {}
}
