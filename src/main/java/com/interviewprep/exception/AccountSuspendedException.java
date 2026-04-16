package com.interviewprep.exception;

public class AccountSuspendedException extends RuntimeException {
    public AccountSuspendedException() {
        super("Your account has been suspended. Please contact an administrator.");
    }
}
