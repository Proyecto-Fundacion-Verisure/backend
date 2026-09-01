package com.verisure.backend.service;

import org.springframework.stereotype.Service;

import com.verisure.backend.dto.activityclosure.ActivityClosureResponse;
import com.verisure.backend.dto.activityclosure.SaveActivityClosureRequest;
import com.verisure.backend.repository.ActivityClosureRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityClosureServiceImpl implements ActivityClosureService  {

    private final ActivityClosureRepository activityClosureRepository;
    private final NotificationService notificationService;
    private final RegistrationLifecycleService registrationLifecycle;

    @Override
    @Transactional
    public ActivityClosureResponse finalizeClosure(Long activityId) {
        // ... pasa el cierre a CLOSED y sella closedAt ...
        registrationLifecycle.closeAllForActivity(activityId);
        notificationService.notifyActivityClosed(activityId);
        return null; //response
    }
    @Override
    public ActivityClosureResponse getClosure(Long activityId){

        return null;

    }
    @Override
    public ActivityClosureResponse saveDraft(Long activityId, SaveActivityClosureRequest request){

        return null;
    }

}
