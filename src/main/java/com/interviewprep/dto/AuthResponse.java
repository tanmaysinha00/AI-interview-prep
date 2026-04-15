package com.interviewprep.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static AuthResponse bearer(String token, long expiresInSeconds) {
        return new AuthResponse(token, "Bearer", expiresInSeconds);
    }
}
