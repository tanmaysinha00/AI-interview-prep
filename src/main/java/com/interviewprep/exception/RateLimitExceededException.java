package com.interviewprep.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String key, int limit, int windowSeconds) {
        super("Rate limit exceeded for '%s': max %d requests per %d seconds".formatted(key, limit, windowSeconds));
    }
}
