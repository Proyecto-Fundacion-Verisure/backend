package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.activityclosure.ActivityClosureResponse;
import com.verisure.backend.dto.activityclosure.ActivityClosureRow;
import com.verisure.backend.dto.activityclosure.SaveActivityClosureRequest;
import com.verisure.backend.repository.ActivityClosureRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivityClosureServiceImpl implements ActivityClosureService {

    private final ActivityClosureRepository activityClosureRepository;
    private final RegistrationLifecycleService registrationLifecycle;

    @Override
    @Transactional(readOnly = true)
    public Page<ActivityClosureRow> findPendingClosure(Pageable pageable) {
        return Page.empty(pageable); // TODO B1-04
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityClosureResponse getByActivity(Long activityId) {
        return null; // TODO B1-04
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
}
