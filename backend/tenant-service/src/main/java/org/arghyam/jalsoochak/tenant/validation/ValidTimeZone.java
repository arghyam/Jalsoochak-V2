package org.arghyam.jalsoochak.tenant.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for timezone strings.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeZoneValidator.class)
@Documented
public @interface ValidTimeZone {
    String message() default "Invalid timezone format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
