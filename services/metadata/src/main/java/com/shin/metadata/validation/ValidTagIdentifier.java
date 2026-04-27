package com.shin.metadata.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TagIdentifierValidator.class)
public @interface ValidTagIdentifier {
    String message() default "Invalid tag identifier";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
