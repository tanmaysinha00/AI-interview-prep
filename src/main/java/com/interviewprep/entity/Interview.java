package com.interviewprep.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.CREATED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> topics;

    // JSON: {"questionCount":10,"initialDifficulty":"MEDIUM","currentDifficulty":"MEDIUM"}
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String config;

    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    // Cached Claude-generated summary — generated once, never regenerated
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_payload", columnDefinition = "jsonb")
    private String summaryPayload;

    // ----------------------------------------------------------------
    // Getters & setters
    // ----------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public InterviewStatus getStatus() { return status; }
    public void setStatus(InterviewStatus status) { this.status = status; }

    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }

    public String getConfig() { return config; }
    public void setConfig(String config) { this.config = config; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public String getSummaryPayload() { return summaryPayload; }
    public void setSummaryPayload(String summaryPayload) { this.summaryPayload = summaryPayload; }
}
