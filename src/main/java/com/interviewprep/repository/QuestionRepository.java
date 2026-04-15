package com.interviewprep.repository;

import com.interviewprep.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findByInterviewIdOrderBySequenceNumber(UUID interviewId);

    long countByInterviewId(UUID interviewId);

    Optional<Question> findByIdAndInterviewId(UUID id, UUID interviewId);

    @Query("SELECT q.topic FROM Question q WHERE q.interviewId = :interviewId ORDER BY q.sequenceNumber")
    List<String> findTopicsByInterviewId(@Param("interviewId") UUID interviewId);

    /**
     * Returns the most recent question that has no corresponding answer yet.
     * Used to make getNextQuestion idempotent — re-fetching returns the same question.
     */
    @Query("""
            SELECT q FROM Question q
            WHERE q.interviewId = :interviewId
              AND NOT EXISTS (SELECT 1 FROM Answer a WHERE a.questionId = q.id)
            ORDER BY q.sequenceNumber ASC
            """)
    Optional<Question> findFirstUnansweredQuestion(@Param("interviewId") UUID interviewId);
}
