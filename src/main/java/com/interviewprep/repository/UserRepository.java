package com.interviewprep.repository;

import com.interviewprep.entity.AuthProvider;
import com.interviewprep.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);

    boolean existsByEmail(String email);
}
