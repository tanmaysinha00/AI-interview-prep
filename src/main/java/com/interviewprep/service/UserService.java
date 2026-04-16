package com.interviewprep.service;

import com.interviewprep.aspect.LogExecutionTime;
import com.interviewprep.dto.AuthResponse;
import com.interviewprep.dto.RegisterRequest;
import com.interviewprep.entity.AuthProvider;
import com.interviewprep.entity.User;
import com.interviewprep.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String adminEmail;
    private final String adminPassword;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${app.admin.email:admin@interviewprep.com}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    // ----------------------------------------------------------------
    // Seed admin on startup
    // ----------------------------------------------------------------

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdmin() {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("ADMIN_PASSWORD not set — skipping admin seed. Set app.admin.password env var.");
            return;
        }
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            return; // already exists
        }
        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setProvider(AuthProvider.LOCAL);
        admin.setDisplayName("Admin");
        admin.setRole(User.Role.ADMIN);
        admin.setStatus(User.Status.ACTIVE);
        userRepository.save(admin);
        log.info("Admin user seeded: {}", adminEmail);
    }

    // ----------------------------------------------------------------
    // UserDetailsService
    // ----------------------------------------------------------------

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    // ----------------------------------------------------------------
    // Register
    // ----------------------------------------------------------------

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
        user.setRole(User.Role.USER);
        user.setStatus(User.Status.PENDING); // requires admin approval

        userRepository.save(user);

        // Return a limited token — frontend should show "awaiting approval" message
        String token = jwtService.generateAccessToken(user);
        return AuthResponse.bearer(token, jwtService.getAccessTokenExpirySeconds());
    }

    // ----------------------------------------------------------------
    // Login tracking
    // ----------------------------------------------------------------

    @LogExecutionTime
    @Transactional
    public void updateLastLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setLastLogin(OffsetDateTime.now());
            user.setLoginCount(user.getLoginCount() + 1);
            userRepository.save(user);
        });
    }
}
