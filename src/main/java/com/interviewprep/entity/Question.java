package com.interviewprep.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "interview_id", nullable = false)
    private UUID interviewId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(nullable = false, length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DifficultyLevel difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionType type;

    // Raw JSON blob matching the question contract from the spec
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_payload", columnDefinition = "jsonb", nullable = false)
    private String questionPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // ----------------------------------------------------------------
    // Getters & setters
    // ----------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInterviewId() { return interviewId; }
    public void setInterviewId(UUID interviewId) { this.interviewId = interviewId; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyLevel difficulty) { this.difficulty = difficulty; }

    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }

    public String getQuestionPayload() { return questionPayload; }
    public void setQuestionPayload(String questionPayload) { this.questionPayload = questionPayload; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
