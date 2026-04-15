package com.interviewprep.repository;

import com.interviewprep.entity.TokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TokenUsageRepository extends JpaRepository<TokenUsage, UUID> {

    @Query("SELECT COALESCE(SUM(t.inputTokens + t.outputTokens), 0) FROM TokenUsage t WHERE t.interviewId = :interviewId")
    long sumTotalTokensByInterviewId(@Param("interviewId") UUID interviewId);
}
