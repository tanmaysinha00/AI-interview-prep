package com.interviewprep.dto;

import com.interviewprep.entity.DifficultyLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateInterviewRequest(
        @NotEmpty @Size(min = 1, max = 10) List<@NotNull String> topics,
        @Min(1) @Max(20) int questionCount,
        @NotNull DifficultyLevel difficulty,
        @Min(0) @Max(40) int yearsOfExperience
) {}
