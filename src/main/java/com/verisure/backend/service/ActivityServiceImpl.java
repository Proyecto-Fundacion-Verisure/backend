package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.activity.ActivityFormResponse;
import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.activity.UpdateActivityRequest;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.mapper.ActivityMapper;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.ActivitySummary;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;
    private final UserRepository userRepository;
    private final RegistrationLifecycleService registrationLifecycleService;

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
    @Transactional(readOnly = true)
    public ActivityFormResponse getForm(Long activityId) {
        Activity activity = findActivityOrThrow(activityId);
        return activityMapper.toFormResponse(activity);
    }

    @Override
    @Transactional
    public ActivityResponse update(Long activityId, UpdateActivityRequest request) {
        Activity activity = findActivityOrThrow(activityId);
        assertEditable(activity);
        activityMapper.updateEntity(request, activity);
        return activityMapper.toResponse(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public void cancel(Long activityId) {
        Activity activity = findActivityOrThrow(activityId);
        assertEditable(activity);

        activity.setStatus(ActivityStatus.CANCELLED);

        // La llamada corre dentro de esta transacción: o se cancelan la
        // actividad y sus inscripciones, o no se cancela ninguna de las dos.
        // Qué estados pasan a CANCELLED lo decide BE3, no esta clase.
        registrationLifecycleService.cancelAllForActivity(activityId);

        activityRepository.save(activity);
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

    @Override
    @Transactional(readOnly = true)
    public Page<ActivitySummary> list(ActivityStatus status, Pageable pageable) {
        return activityRepository.findByStatusWithFavoriteCount(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ActivitySummary> listPendingApproval(Pageable pageable) {
        return activityRepository.findPendingApproval(pageable);
    }

    @Override
    @Transactional
    public ActivityResponse approve(Long activityId) {
        Activity activity = findActivityOrThrow(activityId);
        assertPendingApproval(activity);

        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setReviewNote(null);

        return activityMapper.toResponse(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public ActivityResponse returnToDraft(Long activityId, String note) {
        Activity activity = findActivityOrThrow(activityId);
        assertPendingApproval(activity);

        activity.setStatus(ActivityStatus.DRAFT);
        activity.setReviewNote(note);

        return activityMapper.toResponse(activityRepository.save(activity));
    }

    private Activity findActivityOrThrow(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> NotFoundException.of("Activity", activityId));
    }

    /**
     * Las dos reglas de edición de admin · B2-05: una actividad finalizada no se
     * toca y una cancelada tampoco. El resto —borrador, pendiente, publicada, en
     * curso, llena— se puede editar.
     */
    private void assertEditable(Activity activity) {
        if (activity.getStatus() == ActivityStatus.FINISHED) {
            throw new DomainException(ErrorCode.ACTIVITY_FINISHED);
        }
        if (activity.getStatus() == ActivityStatus.CANCELLED) {
            throw new DomainException(ErrorCode.ACTIVITY_NOT_EDITABLE);
        }
    }

    /**
     * Solo las actividades propuestas por una entidad pasan por revisión ·
     * {@code B2-15}. En cualquier otro estado, aprobar o devolver es un error
     * de conflicto.
     */
    private void assertPendingApproval(Activity activity) {
        if (activity.getStatus() != ActivityStatus.PENDING_APPROVAL) {
            throw new DomainException(ErrorCode.ACTIVITY_NOT_PENDING_APPROVAL);
        }
    }
}