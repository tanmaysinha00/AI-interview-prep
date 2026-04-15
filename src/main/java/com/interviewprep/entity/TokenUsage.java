package com.interviewprep.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "token_usage")
public class TokenUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(name = "input_tokens", nullable = false)
    private int inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private int outputTokens;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ----------------------------------------------------------------
    // Getters & setters
    // ----------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInterviewId() { return interviewId; }
    public void setInterviewId(UUID interviewId) { this.interviewId = interviewId; }

    public int getInputTokens() { return inputTokens; }
    public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }

    public int getOutputTokens() { return outputTokens; }
    public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
