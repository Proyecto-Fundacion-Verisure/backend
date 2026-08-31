package com.verisure.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.repository.projection.SpotInfo;

public interface ActivityRepository extends JpaRepository<Activity, Long>{
    
     default Optional<SpotInfo> findSpotInfo(Long activityId) {
        return Optional.empty();
    }

}
