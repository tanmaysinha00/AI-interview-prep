package com.interviewprep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitAnswerRequest(
        @NotBlank @Size(min = 1, max = 2_000) String answer,
        Integer timeTakenSeconds
) {}
