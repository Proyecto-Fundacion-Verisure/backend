package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.activityclosure.ActivityClosureResponse;
import com.verisure.backend.dto.activityclosure.ActivityClosureRow;
import com.verisure.backend.dto.activityclosure.SaveActivityClosureRequest;

/**
 * El cierre que rellena la Fundación: uno por actividad.
 *
 * <p>La administradora no revisa los formularios de los empleados uno por uno:
 * mira los totales de la actividad —previsto frente a reportado— y cierra una
 * sola vez, lo que arrastra todas las participaciones a {@code CLOSED}.
 *
 * <p>Tarea: B1-04.
 */
public interface ActivityClosureService {

    /** Cola de actividades finalizadas que aún no se han cerrado. Solo administración. */
    Page<ActivityClosureRow> findPendingClosure(Pageable pageable);

    /**
     * Borrador del cierre más los agregados de participación que necesita la
     * administradora para decidir. Los agregados salen de
     * {@link com.verisure.backend.repository.projection.ActivityClosureAggregates},
     * no de llamar al servicio del otro cierre.
     */
    ActivityClosureResponse getByActivity(Long activityId);

    /** Guarda el borrador sin cerrar. El cierre queda en {@code DRAFT}. */
    ActivityClosureResponse saveDraft(Long activityId, SaveActivityClosureRequest request);

    /**
     * Cierra la actividad. <b>No se deshace.</b> Pasa el cierre a {@code CLOSED},
     * sella {@code closedAt} y llama a
     * {@link RegistrationLifecycleService#closeAllForActivity(Long)}.
     *
     * <p><b>No manda el correo:</b> avisa el controlador cuando este método ha
     * vuelto sin lanzar. Ver la regla en {@link NotificationService}.
     */
    ActivityClosureResponse finalizeClosure(Long activityId);
}
