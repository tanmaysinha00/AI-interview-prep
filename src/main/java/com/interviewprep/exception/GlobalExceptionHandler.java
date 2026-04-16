package com.interviewprep.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InterviewNotFoundException.class)
    public ProblemDetail handleInterviewNotFound(InterviewNotFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Interview Not Found");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ProblemDetail handleQuestionNotFound(QuestionNotFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Question Not Found");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(UnauthorizedInterviewAccessException.class)
    public ProblemDetail handleUnauthorizedAccess(UnauthorizedInterviewAccessException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Access Denied");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(TokenBudgetExceededException.class)
    public ProblemDetail handleTokenBudget(TokenBudgetExceededException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Token Budget Exceeded");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ProblemDetail handleRateLimit(RateLimitExceededException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        pd.setTitle("Rate Limit Exceeded");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(ClaudeApiException.class)
    public ProblemDetail handleClaudeApi(ClaudeApiException ex, HttpServletRequest req) {
        log.error("Claude API error: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "AI service temporarily unavailable. Please try again.");
        pd.setTitle("AI Service Error");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, details);
        pd.setTitle("Validation Failed");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Constraint Violation");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(AccountPendingException.class)
    public ProblemDetail handleAccountPending(AccountPendingException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Account Pending Approval");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ProblemDetail handleAccountSuspended(AccountSuspendedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Account Suspended");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication required");
        pd.setTitle("Unauthorized");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
        pd.setTitle("Forbidden");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        log.warn("Illegal state at {}: {}", req.getRequestURI(), ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setInstance(URI.create(req.getRequestURI()));
        return pd;
    }
}
