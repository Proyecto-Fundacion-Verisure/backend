package com.verisure.backend.validation;

import java.time.LocalDate;

import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.activity.UpdateActivityRequest;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidator;


public class ValidDateRangeValidator
    implements ConstraintValidator<ValidDateRange, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        LocalDate startDate;
        LocalDate endDate;
        LocalDate registrationDeadline;

        if (value instanceof CreateActivityRequest create) {
            startDate = create.startDate();
            endDate = create.endDate();
            registrationDeadline = create.registrationDeadline();
        } else if (value instanceof UpdateActivityRequest update) {
            startDate = update.startDate();
            endDate = update.endDate();
            registrationDeadline = update.registrationDeadline();
        } else {
            return true;
        }

        if (startDate == null || endDate == null || registrationDeadline == null) {
            return true;
        }

    context.disableDefaultConstraintViolation();
    boolean valid = true;

    if (endDate.isBefore(startDate)) {
        context.buildConstraintViolationWithTemplate(
            "la fecha de fin no puede ser anterior a la fecha de inicio")
            .addPropertyNode("endDate")
            .addConstraintViolation();
        valid = false;
    }

    if (registrationDeadline.isAfter(startDate)) {
        context.buildConstraintViolationWithTemplate(
            "La fecha de inscripción no puede ser posterior a startDate")
            .addPropertyNode("registrationDeadline")
            .addConstraintViolation();

        valid = false;
    }

    return valid;
    }
}