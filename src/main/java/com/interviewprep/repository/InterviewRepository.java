package com.interviewprep.repository;

import com.interviewprep.entity.Interview;
import com.interviewprep.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByUserIdOrderByStartedAtDesc(UUID userId);

    Optional<Interview> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndStatusAndStartedAtAfter(UUID userId, InterviewStatus status, OffsetDateTime after);

    long countByUserId(UUID userId);

    long countByStartedAtAfter(OffsetDateTime after);

    @Query("SELECT COALESCE(AVG(i.totalScore), 0) FROM Interview i WHERE i.userId = :userId AND i.totalScore IS NOT NULL")
    double avgScoreByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT DATE(i.startedAt) as day, COUNT(i) as cnt
            FROM Interview i
            WHERE i.startedAt >= :since
            GROUP BY DATE(i.startedAt)
            ORDER BY DATE(i.startedAt)
            """)
    List<Object[]> countInterviewsByDay(@Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(DISTINCT i.userId) FROM Interview i WHERE i.startedAt >= :since")
    long countDistinctActiveUsers(@Param("since") OffsetDateTime since);

    @Query("""
            SELECT COUNT(u.id) FROM User u
            WHERE u.role = 'USER'
              AND NOT EXISTS (SELECT 1 FROM Interview i WHERE i.userId = u.id)
            """)
    long countDormantUsers();
}
