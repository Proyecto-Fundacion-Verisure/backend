package com.verisure.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.projection.ClosedParticipationView;

public interface RegistrationRepository extends JpaRepository<Registration, Long>{

    long countByActivityIdAndStatus(Long activityId, RegistrationStatus status);

    List<Registration> findByActivityIdAndStatusOrderByQueuePosition(
            Long activityId, RegistrationStatus status);

    List<ClosedParticipationView> findClosedForDashboard(Integer year, String line);

}
