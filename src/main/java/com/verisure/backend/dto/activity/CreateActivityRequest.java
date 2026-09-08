package com.verisure.backend.dto.activity;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.verisure.backend.validation.ValidDateRange;

@ValidDateRange
public record CreateActivityRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank String description,
    @NotBlank @Size(max = 60) String line,
    @NotBlank @Size(max= 20) String mode,
    @Size(max = 160) String location, 
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull LocalDate registrationDeadline,
    @NotNull Integer hours,
    @NotNull Integer spots,
    @Size(max = 255) String imageUrl) {

}
