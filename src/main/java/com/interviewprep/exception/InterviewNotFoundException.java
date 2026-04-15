package com.interviewprep.exception;

import java.util.UUID;

public class InterviewNotFoundException extends RuntimeException {

    public InterviewNotFoundException(UUID id) {
        super("Interview not found: " + id);
    }
}
