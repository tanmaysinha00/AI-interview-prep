package com.interviewprep.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates required environment variables immediately after the Environment is
 * prepared — before any beans are created. If anything is missing, prints a
 * clear list and stops the application, avoiding cryptic Spring wiring errors.
 */
public class StartupValidator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();

        // Only validate in prod — dev/test have local defaults
        String[] activeProfiles = env.getActiveProfiles();
        boolean isProd = false;
        for (String p : activeProfiles) {
            if ("prod".equals(p)) { isProd = true; break; }
        }
        if (!isProd) return;

        List<String> missing = new ArrayList<>();

        requireEnvVar("DATABASE_URL",         missing);
        requireEnvVar("REDIS_URL",            missing);
        requireEnvVar("JWT_SECRET",           missing);
        requireEnvVar("CLAUDE_API_KEY",       missing);

        if (!missing.isEmpty()) {
            System.err.println("=============================================================");
            System.err.println("APPLICATION STARTUP FAILED — missing required env vars:");
            missing.forEach(v -> System.err.println("  ✗  " + v));
            System.err.println("Set these in Railway → your service → Variables.");
            System.err.println("=============================================================");
            throw new IllegalStateException(
                "Missing required environment variables: " + String.join(", ", missing));
        }

        log.info("StartupValidator: all required env vars present");
    }

    private void requireEnvVar(String name, List<String> missing) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            missing.add(name);
        }
    }
}
