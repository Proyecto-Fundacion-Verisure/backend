package com.verisure.backend.repository.projection;

import java.time.LocalDate;

import com.verisure.backend.entity.enums.ActivityStatus;

public record ActivitySummary(Long id, String title, String partnerName, ActivityStatus status, LocalDate startDate, LocalDate endDate, Integer spots, long favoriteCount) {

}
