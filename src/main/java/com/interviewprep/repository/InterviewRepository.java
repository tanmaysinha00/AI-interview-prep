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
}
