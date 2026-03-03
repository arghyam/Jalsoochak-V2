package org.arghyam.jalsoochak.tenant.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.ZoneId;

/**
 * Validator to check if a timezone string is valid.
 */
public class TimeZoneValidator implements ConstraintValidator<ValidTimeZone, String> {

    @Override
    public void initialize(ValidTimeZone constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Null is validated by @NotBlank
        }

        try {
            ZoneId.of(value);
            return true;
        } catch (Exception e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Invalid timezone: " + value + ". Use format like 'Asia/Kolkata'"
            ).addConstraintViolation();
            return false;
        }
    }
}
