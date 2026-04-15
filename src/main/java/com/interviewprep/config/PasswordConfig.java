package com.interviewprep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Standalone config for PasswordEncoder to break the circular dependency:
 * JwtAuthFilter → UserService → SecurityConfig → JwtAuthFilter
 *
 * PasswordEncoder is now sourced independently by both UserService and SecurityConfig.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
