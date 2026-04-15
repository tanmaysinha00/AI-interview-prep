package com.interviewprep.exception;

import java.util.UUID;

public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(UUID id) {
        super("Question not found: " + id);
    }
}
