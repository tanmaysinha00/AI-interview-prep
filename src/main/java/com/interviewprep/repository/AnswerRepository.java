package com.interviewprep.repository;

import com.interviewprep.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    Optional<Answer> findByQuestionId(UUID questionId);

    @Query("""
           SELECT a FROM Answer a
           WHERE a.questionId IN (
               SELECT q.id FROM Question q WHERE q.interviewId = :interviewId
           )
           ORDER BY a.answeredAt
           """)
    List<Answer> findByInterviewIdOrderByAnsweredAt(@Param("interviewId") UUID interviewId);
}
