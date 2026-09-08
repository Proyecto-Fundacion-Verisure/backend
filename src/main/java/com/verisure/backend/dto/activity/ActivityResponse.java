package com.verisure.backend.dto.activity;

import java.time.LocalDate;
import com.verisure.backend.entity.enums.ActivityStatus;

public record ActivityResponse(
    Long id,
    String title,
    String description,
    String line,
    String mode,
    String location,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate registrationDeadline,
    Integer hours,
    Integer spots,
    String imageUrl,
    ActivityStatus status,
    String partnerName
) {

}
