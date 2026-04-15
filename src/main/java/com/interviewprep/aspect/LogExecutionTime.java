package com.interviewprep.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method (or all methods in a class) for execution-time logging.
 * Applied automatically to all service-layer methods via pointcut in LoggingAspect.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
}
