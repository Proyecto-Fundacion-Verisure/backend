package com.verisure.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;

public interface RegistrationRepository extends JpaRepository<Registration, Long>{

    long countByActivityIdAndStatus(Long activityId, RegistrationStatus status);

    List<Registration> findByActivityIdAndStatusOrderByQueuePosition(
            Long activityId, RegistrationStatus status);

    // findClosedForDashboard vive en ParticipationClosureRepository: la consulta
    // arranca en ParticipationClosure, que es de donde salen las horas reales.

}
