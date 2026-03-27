package org.arghyam.jalsoochak.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that an OTP field contains only digits and has the exact length
 * configured via {@code otp.otp-length}.
 */
@Documented
@Constraint(validatedBy = OtpValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOtp {
    String message() default "otp must be digits only with the expected length";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
