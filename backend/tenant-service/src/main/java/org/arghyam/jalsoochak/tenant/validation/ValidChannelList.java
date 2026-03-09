package org.arghyam.jalsoochak.tenant.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation for channel lists.
 * Validates that all channel codes are valid: BFM, ELM, PDU, IOT, MAN.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ChannelValidator.class)
@Documented
public @interface ValidChannelList {
    String message() default "Invalid channel codes";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
