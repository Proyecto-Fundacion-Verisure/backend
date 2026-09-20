package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.org.OrgActivityRow;
import com.verisure.backend.entity.enums.ActivityStatus;

/**
 * Las actividades del rol entidad.
 *
 * <p>Todas las firmas reciben el correo de quien llama y ninguna un
 * identificador de entidad: ver {@link PartnerResolver}.
 *
 * <p>Dueña: BE3 · Tareas: B2-13 y B2-14.
 */
public interface OrgActivityService {

    /** Una página de las actividades de la entidad, filtrada por estado si se pide. */
    Page<OrgActivityRow> list(ActivityStatus status, Pageable pageable, String userEmail);

    /** Crea una actividad en borrador a nombre de la entidad. */
    OrgActivityRow create(CreateActivityRequest request, String userEmail);

    /** Reescribe una actividad propia que siga en borrador. */
    OrgActivityRow update(Long activityId, CreateActivityRequest request, String userEmail);

    /** Envía a revisión un borrador propio · {@code B2-14}. */
    OrgActivityRow submit(Long activityId, String userEmail);
}
