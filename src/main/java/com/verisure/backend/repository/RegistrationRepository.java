package com.verisure.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;

public interface RegistrationRepository extends JpaRepository<Registration, Long>{

    long countByActivityIdAndStatus(Long activityId, RegistrationStatus status);

    List<Registration> findByActivityIdAndStatusOrderByQueuePosition(
            Long activityId, RegistrationStatus status);

    // findClosedForDashboard vive en ParticipationClosureRepository: la consulta
    // arranca en ParticipationClosure, que es de donde salen las horas reales.

    /**
     * ¿Tiene esta persona una inscripción viva en esta actividad?
     *
     * <p>«Viva» es cualquier estado que no sea {@code CANCELLED}. La regla de
     * negocio es «no puedes volver a apuntarte, salvo que cancelaras tú»: si te
     * rechazaron o ya estás dentro, no; si cancelaste, sí. La llama
     * {@code SpotService} antes de insertar.
     *
     * <p><b>Esto no es una restricción de unicidad, y no puede serlo.</b> Puede
     * haber varias filas de la misma persona y la misma actividad —la que
     * canceló y la nueva—, así que la tabla <b>no</b> lleva constraint sobre
     * {@code (activity_id, user_id)}. Si alguien la añade, rompe la regla.
     */
    boolean existsByActivityIdAndUserIdAndStatusNot(
            Long activityId, Long userId, RegistrationStatus status);

    /** Las inscripciones de una persona, para {@code GET /api/registrations/me} · {@code B3-06}. */
    List<Registration> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Cuántas solicitudes de la actividad ha revisado ya la administradora.
     *
     * <p>Todavía no la consume nadie: es para los contadores del tablero de
     * {@code B3-03}, donde «sin revisar» son las que tienen {@code accepted = false}.
     */
    long countByActivityIdAndAccepted(Long activityId, boolean accepted);

    /**
     * Las inscripciones de una actividad en cualquiera de los estados dados.
     *
     * <p>La usan las dos operaciones en masa de {@code RegistrationLifecycleService}
     * · {@code B3-06}: cancelar pide {@code WAITLISTED} y {@code CONFIRMED}, cerrar
     * pide {@code PENDING_CLOSURE}.
     */
    List<Registration> findByActivityIdAndStatusIn(Long activityId, List<RegistrationStatus> statuses);

}
