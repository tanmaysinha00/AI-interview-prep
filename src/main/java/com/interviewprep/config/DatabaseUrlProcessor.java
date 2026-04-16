package com.interviewprep.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.Map;

/**
 * Parses the DATABASE_URL environment variable provided by Railway (and Heroku).
 * Railway provides: postgresql://user:pass@host:port/db
 * Spring Boot needs: jdbc:postgresql://host:port/db  +  separate user/password
 *
 * Runs before any beans are created, so HikariCP always gets a valid JDBC URL.
 */
public class DatabaseUrlProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String rawUrl = System.getenv("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) return;

        try {
            // Normalise postgres:// → postgresql:// so java.net.URI parses it cleanly
            URI uri = new URI(rawUrl.replace("postgres://", "postgresql://"));

            String host     = uri.getHost();
            int    port     = uri.getPort() > 0 ? uri.getPort() : 5432;
            String db       = uri.getPath().replaceFirst("^/", "");
            String userInfo = uri.getUserInfo();            // "user:password"
            String user     = userInfo.split(":", 2)[0];
            String password = userInfo.split(":", 2)[1];

            String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, db);

            // addFirst so this overrides anything in application-prod.properties
            environment.getPropertySources().addFirst(new MapPropertySource(
                "databaseUrlProcessor",
                Map.of(
                    "spring.datasource.url",      jdbcUrl,
                    "spring.datasource.username", user,
                    "spring.datasource.password", password
                )
            ));
        } catch (Exception ignored) {
            // If parsing fails, let Spring Boot report the original datasource error
        }
    }
}
