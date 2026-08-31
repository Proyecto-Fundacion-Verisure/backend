package com.verisure.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.ParticipationClosure;

public interface ParticipationClosureRepository extends JpaRepository<ParticipationClosure, Long> {

    Optional<ParticipationClosure> findByRegistrationId(Long registrationId);

}
