package com.verisure.backend.dto.activity;
import com.verisure.backend.entity.enums.ActivityStatus;
import java.time.LocalDate;

public record ActivityCardResponse(
    Long id,
    String title,
    String partnerName,
    String line,
    String mode,
    String location,
    LocalDate startDate,
    LocalDate endDate,
    Integer hours,
    Integer spots,
    String imageUrl,
    ActivityStatus status) {

}
 