package com.interviewprep.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    /**
     * Matches all public methods in the service package (catch-all for the service layer)
     * and any method annotated with @LogExecutionTime.
     */
    @Pointcut("within(com.interviewprep.service..*) || @annotation(com.interviewprep.aspect.LogExecutionTime)")
    public void loggableMethod() {}

    @Around("loggableMethod()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger log = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String method = joinPoint.getSignature().toShortString();
        long start = System.currentTimeMillis();

        log.debug("Entering: {}", method);

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Completed: {} in {}ms", method, elapsed);
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Failed: {} in {}ms — {}: {}", method, elapsed,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }
}
