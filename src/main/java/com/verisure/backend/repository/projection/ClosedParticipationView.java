package com.verisure.backend.repository.projection;

import java.time.LocalDate;

public record ClosedParticipationView(String department, String organization, String line,
        Integer actualHours, LocalDate endDate) {

}
