package com.interviewprep.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Parses DATABASE_URL (provided by Railway/Heroku) before any Spring beans start.
 * Railway provides: postgresql://user:pass@host:port/db
 * Spring Boot needs: jdbc:postgresql://host:port/db  +  separate user/password
 *
 * Also parses REDIS_URL if it contains a username segment ("default") that
 * some Lettuce versions cannot handle — strips it and injects individual props.
 */
public class DatabaseUrlProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        parseDatabase(environment);
        parseRedis(environment);
    }

    private void parseDatabase(ConfigurableEnvironment environment) {
        String rawUrl = System.getenv("DATABASE_URL");
        if (rawUrl == null || rawUrl.isBlank()) return;

        try {
            // Normalise postgres:// → postgresql:// for java.net.URI
            URI uri = new URI(rawUrl.replace("postgres://", "postgresql://"));

            String host     = uri.getHost();
            int    port     = uri.getPort() > 0 ? uri.getPort() : 5432;
            String db       = uri.getPath().replaceFirst("^/", "");
            String userInfo = uri.getUserInfo();

            if (userInfo == null || !userInfo.contains(":")) {
                System.err.println("[DatabaseUrlProcessor] DATABASE_URL has no user:password — skipping datasource injection");
                return;
            }

            String user     = URLDecoder.decode(userInfo.split(":", 2)[0], StandardCharsets.UTF_8);
            String password = URLDecoder.decode(userInfo.split(":", 2)[1], StandardCharsets.UTF_8);
            String jdbcUrl  = String.format("jdbc:postgresql://%s:%d/%s", host, port, db);

            environment.getPropertySources().addFirst(new MapPropertySource(
                "databaseUrlProcessor",
                Map.of(
                    "spring.datasource.url",                   jdbcUrl,
                    "spring.datasource.username",              user,
                    "spring.datasource.password",              password,
                    "spring.datasource.driver-class-name",     "org.postgresql.Driver"
                )
            ));
        } catch (Exception e) {
            System.err.println("[DatabaseUrlProcessor] Failed to parse DATABASE_URL: " + e.getMessage());
            // Do not rethrow — let Spring Boot report a clear datasource missing error
        }
    }

    private void parseRedis(ConfigurableEnvironment environment) {
        String redisUrl = System.getenv("REDIS_URL");
        if (redisUrl == null || redisUrl.isBlank()) return;

        try {
            // Lettuce handles redis:// and rediss:// natively.
            // However, Railway sometimes emits redis://default:pass@host:port where
            // "default" is the ACL username — Lettuce ≥6 handles this fine, but
            // inject spring.data.redis.url directly so Spring Boot picks it up.
            environment.getPropertySources().addFirst(new MapPropertySource(
                "redisUrlProcessor",
                Map.of("spring.data.redis.url", redisUrl)
            ));
        } catch (Exception e) {
            System.err.println("[DatabaseUrlProcessor] Failed to process REDIS_URL: " + e.getMessage());
        }
    }
}
