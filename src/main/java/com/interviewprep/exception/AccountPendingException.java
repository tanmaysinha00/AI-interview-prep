package com.interviewprep.exception;

public class AccountPendingException extends RuntimeException {
    public AccountPendingException() {
        super("Your account is pending approval. Please wait for an administrator to activate it.");
    }
}
