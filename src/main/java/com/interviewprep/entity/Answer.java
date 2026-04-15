package com.interviewprep.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false, unique = true)
    private UUID questionId;

    @Column(name = "user_answer", nullable = false, columnDefinition = "TEXT")
    private String userAnswer;

    // Raw JSON blob: the full evaluation response from Claude
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_payload", columnDefinition = "jsonb")
    private String evaluationPayload;

    @Column(precision = 4, scale = 2)
    private BigDecimal score;

    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private OffsetDateTime answeredAt = OffsetDateTime.now();

    // ----------------------------------------------------------------
    // Getters & setters
    // ----------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getEvaluationPayload() { return evaluationPayload; }
    public void setEvaluationPayload(String evaluationPayload) { this.evaluationPayload = evaluationPayload; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public Integer getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(Integer timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }

    public OffsetDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(OffsetDateTime answeredAt) { this.answeredAt = answeredAt; }
}
