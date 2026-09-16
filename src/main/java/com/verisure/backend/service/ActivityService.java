package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.activity.ActivityFormResponse;
import com.verisure.backend.dto.activity.ActivityResponse;
import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.activity.UpdateActivityRequest;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.repository.projection.ActivitySummary;

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

    /**
     * El listado de actividades de administración, con el recuento de favoritos.
     *
     * <p>Sin filtro devuelve todos los estados, incluidos {@code DRAFT} y
     * {@code CANCELLED}: es el listado de la Fundación, no el catálogo.
     */
    Page<ActivitySummary> list(ActivityStatus status, Pageable pageable);

    /**
     * La cola de actividades propuestas por una entidad y pendientes de revisión
     * · {@code B2-15}, por antigüedad (la más antigua primero).
     */
    Page<ActivitySummary> listPendingApproval(Pageable pageable);

    /**
     * Aprueba una actividad propuesta y la publica · {@code B2-15}.
     *
     * <p>Solo desde {@code PENDING_APPROVAL}: en cualquier otro estado lanza
     * {@code ACTIVITY_NOT_PENDING_APPROVAL} (409) y no cambia nada.
     */
    ActivityResponse approve(Long activityId);

    /**
     * Devuelve una actividad propuesta a su entidad · {@code B2-15}.
     *
     * <p>Solo desde {@code PENDING_APPROVAL}: en cualquier otro estado lanza
     * {@code ACTIVITY_NOT_PENDING_APPROVAL} (409). La pasa a {@code DRAFT} y
     * guarda el comentario en {@code reviewNote}. El aviso lo orquesta el
     * controlador, fuera de la transacción, y la entidad ya no edita hasta que
     * le devuelven la actividad.
     */
    ActivityResponse returnToDraft(Long activityId, String note);
}
