package com.interviewprep.controller;

import com.interviewprep.aspect.RateLimited;
import com.interviewprep.dto.*;
import com.interviewprep.entity.User;
import com.interviewprep.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /** Create a new interview session. Rate-limited: 5 per hour per user. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimited(limit = 5, windowSeconds = 3600, key = "interviews")
    public InterviewResponse createInterview(
            @Valid @RequestBody CreateInterviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        return interviewService.createInterview(request, user.getId());
    }

    /** List all interviews for the authenticated user. */
    @GetMapping
    public List<InterviewResponse> getUserInterviews(@AuthenticationPrincipal User user) {
        return interviewService.getUserInterviews(user.getId());
    }

    /** Get the next question for an in-progress interview. */
    @GetMapping("/{id}/next-question")
    public QuestionResponse getNextQuestion(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return interviewService.getNextQuestion(id, user.getId());
    }

    /** Submit an answer to a specific question. */
    @PostMapping("/{id}/questions/{qId}/answer")
    public EvaluationResponse submitAnswer(
            @PathVariable UUID id,
            @PathVariable UUID qId,
            @Valid @RequestBody SubmitAnswerRequest request,
            @AuthenticationPrincipal User user
    ) {
        return interviewService.submitAnswer(id, qId, request, user.getId());
    }

    /** Get the summary for a completed interview (triggers one final Claude call). */
    @GetMapping("/{id}/summary")
    public InterviewSummaryResponse getInterviewSummary(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return interviewService.getInterviewSummary(id, user.getId());
    }
}
