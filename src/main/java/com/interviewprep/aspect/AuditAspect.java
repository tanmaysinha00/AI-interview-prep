package com.interviewprep.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;

/**
 * Writes structured audit events to the audit log channel after successful
 * execution of methods annotated with @AuditLog.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @AfterReturning("@annotation(com.interviewprep.aspect.AuditLog)")
    public void logAuditEvent(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditLog annotation = method.getAnnotation(AuditLog.class);

        String action = annotation.action().isBlank()
                ? method.getName()
                : annotation.action();

        String principal = resolveUsername();

        auditLog.info("action={} user={} class={} method={} timestamp={}",
                action,
                principal,
                joinPoint.getTarget().getClass().getSimpleName(),
                method.getName(),
                Instant.now());
    }

    private String resolveUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }
}
