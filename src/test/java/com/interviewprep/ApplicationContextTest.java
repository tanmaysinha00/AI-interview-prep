package com.interviewprep;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Loads the full Spring application context using H2 (no Docker, no network).
 *
 * Purpose: catch bean wiring failures, missing @Value bindings, and any startup
 * error BEFORE it reaches Railway. This is the test that would have caught every
 * repeated prod crash (missing datasource, JPA factory not found, etc.) locally.
 *
 * Runs as part of ./mvnw test so the pre-push hook blocks bad pushes.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // H2 in PostgreSQL compatibility mode — no Docker required
    "spring.datasource.url=jdbc:h2:mem:contexttest;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",

    // Redis: Lettuce connects lazily — a non-existent port is fine for context loading
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=16399",

    // Flyway not needed — Hibernate creates schema from entities above
    "spring.flyway.enabled=false"
})
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // If Spring fails to wire any bean, this test fails here with the exact
        // error — no guessing from Railway logs needed.
    }
}
