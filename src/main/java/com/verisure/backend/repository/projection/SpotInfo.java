package com.verisure.backend.repository.projection;

import java.time.LocalDate;

public record SpotInfo(Integer spots, long confirmed, LocalDate registrationDeadline) {

}
