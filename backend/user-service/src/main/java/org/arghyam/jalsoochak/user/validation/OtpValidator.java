package org.arghyam.jalsoochak.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.arghyam.jalsoochak.user.config.properties.OtpProperties;
import org.springframework.stereotype.Component;

@Component
public class OtpValidator implements ConstraintValidator<ValidOtp, String> {

    private final OtpProperties otpProperties;

    public OtpValidator(OtpProperties otpProperties) {
        this.otpProperties = otpProperties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotBlank handles null/blank
        int expectedLength = otpProperties.otpLength();
        if (value.length() != expectedLength) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "otp must be exactly " + expectedLength + " digits").addConstraintViolation();
            return false;
        }
        return value.chars().allMatch(ch -> ch >= '0' && ch <= '9');
    }
}
