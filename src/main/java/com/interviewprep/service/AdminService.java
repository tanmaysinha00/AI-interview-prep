package com.interviewprep.service;

import com.interviewprep.dto.AdminUserResponse;
import com.interviewprep.dto.PlatformMetricsResponse;
import com.interviewprep.entity.User;
import com.interviewprep.repository.InterviewRepository;
import com.interviewprep.repository.TokenUsageRepository;
import com.interviewprep.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final TokenUsageRepository tokenUsageRepository;

    public AdminService(
            UserRepository userRepository,
            InterviewRepository interviewRepository,
            TokenUsageRepository tokenUsageRepository
    ) {
        this.userRepository = userRepository;
        this.interviewRepository = interviewRepository;
        this.tokenUsageRepository = tokenUsageRepository;
    }

    // ----------------------------------------------------------------
    // User list with per-user metrics
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findByRoleOrderByCreatedAtDesc(User.Role.USER)
                .stream()
                .map(this::toAdminUserResponse)
                .toList();
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        long interviews = interviewRepository.countByUserId(user.getId());
        double avgScore = interviewRepository.avgScoreByUserId(user.getId());
        long tokens = tokenUsageRepository.sumTotalTokensByUserId(user.getId());
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getLoginCount(),
                interviews,
                Math.round(avgScore * 10.0) / 10.0,
                tokens,
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }

    // ----------------------------------------------------------------
    // Approve / Reject / Suspend
    // ----------------------------------------------------------------

    @Transactional
    public AdminUserResponse updateUserStatus(UUID userId, User.Status newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setStatus(newStatus);
        userRepository.save(user);
        return toAdminUserResponse(user);
    }

    // ----------------------------------------------------------------
    // Platform metrics
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public PlatformMetricsResponse getPlatformMetrics() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime sevenDaysAgo  = now.minusDays(7);
        OffsetDateTime thirtyDaysAgo = now.minusDays(30);

        long totalUsers     = userRepository.countByStatus(User.Status.ACTIVE)
                            + userRepository.countByStatus(User.Status.PENDING)
                            + userRepository.countByStatus(User.Status.SUSPENDED);
        long pendingUsers   = userRepository.countByStatus(User.Status.PENDING);
        long activeUsers    = userRepository.countByStatus(User.Status.ACTIVE);
        long suspendedUsers = userRepository.countByStatus(User.Status.SUSPENDED);

        long newLast7  = userRepository.countByCreatedAtAfter(sevenDaysAgo);
        long newLast30 = userRepository.countByCreatedAtAfter(thirtyDaysAgo);

        long activeLast7  = interviewRepository.countDistinctActiveUsers(sevenDaysAgo);
        long activeLast30 = interviewRepository.countDistinctActiveUsers(thirtyDaysAgo);
        long dormant      = interviewRepository.countDormantUsers();

        // Power users: 3+ interviews
        long powerUsers = userRepository.findByRoleOrderByCreatedAtDesc(User.Role.USER)
                .stream()
                .filter(u -> interviewRepository.countByUserId(u.getId()) >= 3)
                .count();

        long totalInterviews   = interviewRepository.count();
        long interviewsLast7   = interviewRepository.countByStartedAtAfter(sevenDaysAgo);
        long totalTokens       = tokenUsageRepository.sumAllTokens();

        // Time series
        List<PlatformMetricsResponse.DayCount> newUsersByDay =
                userRepository.countNewUsersByDay(thirtyDaysAgo)
                        .stream()
                        .map(row -> new PlatformMetricsResponse.DayCount(
                                row[0].toString(), ((Number) row[1]).longValue()))
                        .toList();

        List<PlatformMetricsResponse.DayCount> interviewsByDay =
                interviewRepository.countInterviewsByDay(thirtyDaysAgo)
                        .stream()
                        .map(row -> new PlatformMetricsResponse.DayCount(
                                row[0].toString(), ((Number) row[1]).longValue()))
                        .toList();

        return new PlatformMetricsResponse(
                totalUsers, pendingUsers, activeUsers, suspendedUsers,
                newLast7, newLast30,
                activeLast7, activeLast30, dormant, powerUsers,
                totalInterviews, interviewsLast7, totalTokens,
                newUsersByDay, interviewsByDay
        );
    }
}
