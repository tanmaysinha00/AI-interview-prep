package com.interviewprep.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enforces a Redis-backed rate limit on the annotated method.
 * Uses a Lua script for atomic increment + TTL.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /** Maximum number of calls allowed within the window. */
    int limit();

    /** Time window in seconds. */
    int windowSeconds();

    /**
     * Logical key suffix for the rate-limit bucket.
     * The actual Redis key is: rl:{userId}:{key}
     */
    String key() default "default";
}
