package com.verisure.backend.validation;

import com.verisure.backend.dto.activity.CreateActivityRequest;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidator;


public class ValidDateRangeValidator
    implements ConstraintValidator<ValidDateRange, CreateActivityRequest> {

    @Override 
    public boolean isValid(CreateActivityRequest request, ConstraintValidatorContext context) {
        if (request.startDate() == null || request.endDate() == null || request.registrationDeadline() == null) {
            return true;
        }
    
    context.disableDefaultConstraintViolation();
    boolean valid = true;

    if (request.endDate().isBefore(request.startDate())) {
        context.buildConstraintViolationWithTemplate(
            "la fecha de fin no puede ser anterior a la fecha de inicio")
            .addPropertyNode("endDate")
            .addConstraintViolation();
        valid = false;
    }

    if (request.registrationDeadline().isAfter(request.startDate())) {
        context.buildConstraintViolationWithTemplate(
            "La fecha de inscripción no puede ser posterior a startDate")
            .addPropertyNode("registrationDeadline")
            .addConstraintViolation();

        valid = false;
    }

    return valid;
    }
}