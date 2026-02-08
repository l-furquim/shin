package com.shin.metadata.validation;

import com.shin.metadata.dto.TagIdentifier;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TagIdentifierValidator implements ConstraintValidator<ValidTagIdentifier, TagIdentifier> {

    private static final String TAG_NAME_PATTERN = "^[a-zA-Z0-9_-]+$";
    private static final int MAX_TAG_NAME_LENGTH = 50;

    @Override
    public boolean isValid(TagIdentifier tagIdentifier, ConstraintValidatorContext context) {
        if (tagIdentifier == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (tagIdentifier.id() == null && (tagIdentifier.name() == null || tagIdentifier.name().isBlank())) {
            context.buildConstraintViolationWithTemplate("Either id or name must be provided")
                .addConstraintViolation();
            return false;
        }

        if (tagIdentifier.name() != null && !tagIdentifier.name().isBlank()) {
            String trimmedName = tagIdentifier.name().trim();

            if (trimmedName.length() > MAX_TAG_NAME_LENGTH) {
                context.buildConstraintViolationWithTemplate(
                    String.format("Tag name must not exceed %d characters", MAX_TAG_NAME_LENGTH))
                    .addPropertyNode("name")
                    .addConstraintViolation();
                return false;
            }

            if (!trimmedName.matches(TAG_NAME_PATTERN)) {
                context.buildConstraintViolationWithTemplate(
                    "Tag name must contain only alphanumeric characters, hyphens, and underscores")
                    .addPropertyNode("name")
                    .addConstraintViolation();
                return false;
            }
        }

        return true;
    }
}
