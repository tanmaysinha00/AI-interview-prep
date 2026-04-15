package com.interviewprep.exception;

import java.util.UUID;

public class UnauthorizedInterviewAccessException extends RuntimeException {

    public UnauthorizedInterviewAccessException(UUID interviewId) {
        super("Access denied to interview: " + interviewId);
    }
}
