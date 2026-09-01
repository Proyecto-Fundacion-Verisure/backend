package com.verisure.backend.service;

import com.verisure.backend.dto.activityclosure.ActivityClosureResponse;
import com.verisure.backend.dto.activityclosure.SaveActivityClosureRequest;

public interface ActivityClosureService {

    ActivityClosureResponse getClosure(Long activityId);

    ActivityClosureResponse saveDraft(Long activityId, SaveActivityClosureRequest request);

    ActivityClosureResponse finalizeClosure(Long activityId);

}
