package com.interviewprep.aspect;

import com.interviewprep.exception.RateLimitExceededException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Enforces Redis-backed rate limits declared with @RateLimited.
 * Uses an atomic Lua script: increment counter, set TTL on first call.
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    // Returns 1 if under limit, 0 if exceeded
    private static final String RATE_LIMIT_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            if current > tonumber(ARGV[1]) then
                return 0
            end
            return 1
            """;

    private final RedisTemplate<String, String> redisTemplate;
    private final DefaultRedisScript<Long> script;

    public RateLimitAspect(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
    }

    @Before("@annotation(com.interviewprep.aspect.RateLimited)")
    public void enforceRateLimit(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimited annotation = method.getAnnotation(RateLimited.class);

        String userId = resolveUserId();
        String redisKey = "rl:%s:%s".formatted(userId, annotation.key());

        Long result = redisTemplate.execute(
                script,
                List.of(redisKey),
                String.valueOf(annotation.limit()),
                String.valueOf(annotation.windowSeconds())
        );

        if (result == null || result == 0L) {
            log.warn("Rate limit exceeded for user={} key={} limit={}/{}s",
                    userId, annotation.key(), annotation.limit(), annotation.windowSeconds());
            throw new RateLimitExceededException(annotation.key(), annotation.limit(), annotation.windowSeconds());
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
