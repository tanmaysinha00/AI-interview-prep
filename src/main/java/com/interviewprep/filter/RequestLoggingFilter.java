package com.interviewprep.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String uri = request.getRequestURI();

            // Skip actuator/health noise
            if (!uri.startsWith("/actuator")) {
                log.info("method={} uri={} status={} duration={}ms",
                        request.getMethod(), uri, response.getStatus(), duration);
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip logging for static resources
        String uri = request.getRequestURI();
        return uri.startsWith("/swagger-ui") || uri.startsWith("/api-docs");
    }
}
