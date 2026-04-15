package com.interviewprep.entity;

public enum DifficultyLevel {
    EASY, MEDIUM, HARD, EXPERT;

    public DifficultyLevel increase() {
        return switch (this) {
            case EASY   -> MEDIUM;
            case MEDIUM -> HARD;
            case HARD   -> EXPERT;
            case EXPERT -> EXPERT;
        };
    }

    public DifficultyLevel decrease() {
        return switch (this) {
            case EASY   -> EASY;
            case MEDIUM -> EASY;
            case HARD   -> MEDIUM;
            case EXPERT -> HARD;
        };
    }
}
