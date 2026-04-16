package com.interviewprep.repository;

import com.interviewprep.entity.AuthProvider;
import com.interviewprep.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);

    boolean existsByEmail(String email);

    List<User> findByRoleOrderByCreatedAtDesc(User.Role role);

    long countByStatus(User.Status status);

    long countByCreatedAtAfter(OffsetDateTime after);

    long countByLastLoginAfter(OffsetDateTime after);

    @Query("""
            SELECT DATE(u.createdAt) as day, COUNT(u) as cnt
            FROM User u
            WHERE u.createdAt >= :since AND u.role = 'USER'
            GROUP BY DATE(u.createdAt)
            ORDER BY DATE(u.createdAt)
            """)
    List<Object[]> countNewUsersByDay(@Param("since") OffsetDateTime since);
}
