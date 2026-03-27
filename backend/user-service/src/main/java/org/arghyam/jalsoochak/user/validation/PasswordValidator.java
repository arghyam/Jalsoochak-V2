package org.arghyam.jalsoochak.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // 8–64 chars, at least: 1 uppercase, 1 lowercase, 1 digit, 1 special char
    private static final Pattern POLICY = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // @NotBlank handles the null/blank case
        return POLICY.matcher(value).matches();
    }
}
