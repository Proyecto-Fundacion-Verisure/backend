package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.activityclosure.ActivityClosureResponse;
import com.verisure.backend.dto.activityclosure.ActivityClosureRow;
import com.verisure.backend.dto.activityclosure.SaveActivityClosureRequest;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.ActivityClosure;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ActivityClosureRepository;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.projection.ActivityClosureAggregates;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityClosureServiceImpl implements ActivityClosureService {

    private final ActivityClosureRepository activityClosureRepository;
    private final ActivityRepository activityRepository;
    private final ParticipationClosureRepository participationClosureRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationLifecycleService registrationLifecycle;

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityClosureRow> findPendingClosure(Pageable pageable) {
        return activityRepository.findPendingClosure(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityClosureResponse getByActivity(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> NotFoundException.of("actividad", activityId));
        ActivityClosure closure = activityClosureRepository.findByActivityId(activityId)
                .orElse(null);
        return toResponse(activity, closure);
    }

    @Override
    @Transactional
    public ActivityClosureResponse saveDraft(Long activityId, SaveActivityClosureRequest request) {
        return null; // TODO B1-04
    }

    @Override
    @Transactional
    public ActivityClosureResponse finalizeClosure(Long activityId) {
        // TODO B1-04 · pasar el cierre a CLOSED y sellar closedAt

        // Firma cruzada: BE1 no toca ni una fila de registrations.
        registrationLifecycle.closeAllForActivity(activityId);

        // El aviso NO va aquí: lo lanza el controlador cuando este método vuelve.
        return null; // TODO B1-04
    }

    /**
     * El detalle del cierre con sus agregados de participación.
     *
     * <p>Lo comparten {@code getByActivity}, {@code saveDraft} y
     * {@code finalizeClosure}: las tres devuelven el mismo {@code record}, y las
     * dos cifras fiscales salen de las mismas dos consultas.
     */
    private ActivityClosureResponse toResponse(Activity activity, ActivityClosure closure) {
        ActivityClosureAggregates agg = participationClosureRepository
                .findAggregatesByActivityId(activity.getId());
        long confirmed = registrationRepository.countByActivityIdAndStatus(
                activity.getId(), RegistrationStatus.CONFIRMED);
        return ActivityClosureResponse.from(closure, activity, agg, confirmed);
    }
}
