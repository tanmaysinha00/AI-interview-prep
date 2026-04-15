package com.interviewprep.exception;

import java.util.UUID;

public class TokenBudgetExceededException extends RuntimeException {

    public TokenBudgetExceededException(UUID interviewId, long used, long budget) {
        super("Token budget exceeded for interview %s: used %d / %d".formatted(interviewId, used, budget));
    }
}
