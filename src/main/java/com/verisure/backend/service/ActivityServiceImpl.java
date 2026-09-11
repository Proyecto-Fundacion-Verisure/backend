package com.verisure.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.mapper.ActivityMapper;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ActivityResponse create(CreateActivityRequest request, String createdByEmail) {
        User creator = userRepository.findByEmail(createdByEmail)
                .orElseThrow(() -> NotFoundException.of("User", createdByEmail));
        Activity activity = activityMapper.toEntity(request);
        activity.setCreatedBy(creator);
        return activityMapper.toResponse(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public ActivityResponse publish(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> NotFoundException.of("Activity", activityId));
        if (activity.getStatus() != ActivityStatus.DRAFT) {
            throw new DomainException(ErrorCode.ACTIVITY_NOT_EDITABLE);
        }
        activity.setStatus(ActivityStatus.PUBLISHED);
        return activityMapper.toResponse(activityRepository.save(activity));
    }
}