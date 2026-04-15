package com.interviewprep.service;

import com.interviewprep.aspect.LogExecutionTime;
import com.interviewprep.dto.AuthResponse;
import com.interviewprep.dto.RegisterRequest;
import com.interviewprep.entity.AuthProvider;
import com.interviewprep.entity.User;
import com.interviewprep.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    @LogExecutionTime
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);
        user.setDisplayName(request.displayName());

        userRepository.save(user);

        String token = jwtService.generateAccessToken(user);
        return AuthResponse.bearer(token, jwtService.getAccessTokenExpirySeconds());
    }

    @LogExecutionTime
    @Transactional
    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(java.time.OffsetDateTime.now());
            userRepository.save(user);
        });
    }
}
