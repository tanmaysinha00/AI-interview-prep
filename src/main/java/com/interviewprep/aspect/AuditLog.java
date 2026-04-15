package com.interviewprep.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for structured audit logging.
 * The aspect writes a JSON audit event with user context and action description.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** Human-readable description of the business event. */
    String action() default "";
}
