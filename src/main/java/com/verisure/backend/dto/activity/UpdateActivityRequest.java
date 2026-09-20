package com.verisure.backend.dto.activity;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.verisure.backend.validation.ValidDateRange;

/**
 * El PUT de la administración: los mismos campos que {@link CreateActivityRequest},
 * porque el formulario de edición es el mismo que el de creación.
 *
 * <p>Con {@code @ValidDateRange} aplicado aquí también, porque la fecha se puede
 * romper al editar igual que al crear.
 */
@ValidDateRange
public record UpdateActivityRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank String description,
    @NotBlank @Size(max = 60) String line,
    @NotBlank @Size(max= 20) String mode,
    @Size(max = 160) String location,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull LocalDate registrationDeadline,
    @NotNull Integer hours,
    @NotNull Integer spots) {

}