package com.verisure.backend.service;

import com.verisure.backend.dto.activity.ActivityFormResponse;
import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.activity.UpdateActivityRequest;

public interface ActivityService {
    ActivityResponse create(CreateActivityRequest request, String createdByEmail);

    /**
     * Carga el formulario de edición · B2-05.
     *
     * <p>Funciona con cualquier estado, incluidos {@code DRAFT} y
     * {@code CANCELLED}: es el formulario de administración, no el catálogo.
     */
    ActivityFormResponse getForm(Long activityId);

    /**
     * Actualiza los datos de la actividad · B2-05.
     *
     * <p>Mismas validaciones que la creación. Una actividad <b>finalizada</b> no
     * se edita ({@code ACTIVITY_FINISHED}) y una <b>cancelada</b> tampoco
     * ({@code ACTIVITY_NOT_EDITABLE}).
     */
    ActivityResponse update(Long activityId, UpdateActivityRequest request);

    /**
     * Cancela la actividad y sus inscripciones vivas · B2-05.
     *
     * <p>Pasa la actividad a {@code CANCELLED} y llama a
     * {@link RegistrationLifecycleService#cancelAllForActivity} dentro de la
     * misma transacción. El aviso lo orquesta el controlador, fuera.
     */
    void cancel(Long activityId);

    ActivityResponse publish(Long activityId);
}
