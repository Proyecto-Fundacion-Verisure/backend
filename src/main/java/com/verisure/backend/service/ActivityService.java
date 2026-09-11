package com.verisure.backend.service;

import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;

public interface ActivityService {
    ActivityResponse create(CreateActivityRequest request, String createdByEmail);

    ActivityResponse publish(Long activityId);
}
