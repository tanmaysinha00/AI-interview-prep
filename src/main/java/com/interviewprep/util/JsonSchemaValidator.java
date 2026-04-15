package com.interviewprep.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates deserialized DTOs against their jakarta.validation constraints.
 * Used by ClaudeResponseParser to confirm parsed Claude responses match the expected schema.
 */
@Component
public class JsonSchemaValidator {

    private final Validator validator;

    public JsonSchemaValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates an object against its bean validation constraints.
     *
     * @param object the object to validate
     * @param <T>    the type
     * @throws IllegalArgumentException if any constraint violations are found
     */
    public <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Schema validation failed: " + message);
        }
    }

    /**
     * Returns true if the object passes all validation constraints.
     */
    public <T> boolean isValid(T object) {
        return validator.validate(object).isEmpty();
    }
}
