package com.verisure.backend.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.projection.RegistrationCounts;
import com.verisure.backend.repository.projection.RegistrationRow;

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
     * Las inscripciones de una actividad en cualquiera de los estados dados.
     *
     * <p>La usan las dos operaciones en masa de {@code RegistrationLifecycleService}
     * · {@code B3-06}: cancelar pide {@code WAITLISTED} y {@code CONFIRMED}, cerrar
     * pide {@code PENDING_CLOSURE}.
     */
    List<Registration> findByActivityIdAndStatusIn(Long activityId, List<RegistrationStatus> statuses);

    /**
     * Tablero de administración: inscripciones con datos de la persona y sus
     * horas del año · {@code B3-03}.
     *
     * <p>Tres cosas que alguien desharía por error: el {@code cast} de
     * {@code organization}, porque en la entidad es un enumerado y el
     * {@code record} espera {@code String}; el año como parámetro en vez de
     * función de fecha; y la {@code countQuery} escrita a mano, porque la
     * derivada no sabe quitar la subconsulta del {@code select}.
     */
    @Query(value = """
            select new com.verisure.backend.repository.projection.RegistrationRow(
                r.id,
                r.user.fullName,
                r.user.department,
                cast(r.user.organization as string),
                r.status,
                r.accepted,
                r.queuePosition,
                (select coalesce(sum(pc.actualHours), 0L)
                   from ParticipationClosure pc
                   where pc.registration.user.id = r.user.id
                     and pc.registration.status = com.verisure.backend.entity.enums.RegistrationStatus.CLOSED
                     and year(pc.registration.activity.endDate) = :year),
                r.decidedAt,
                r.createdAt)
            from Registration r
            where (:activityId is null or r.activity.id = :activityId)
              and (:status is null or r.status = :status)
            order by r.createdAt desc
            """,
            countQuery = """
            select count(r)
            from Registration r
            where (:activityId is null or r.activity.id = :activityId)
              and (:status is null or r.status = :status)
            """)
    Page<RegistrationRow> findAdminDashboard(
            @Param("activityId") Long activityId,
            @Param("status") RegistrationStatus status,
            @Param("year") int year,
            Pageable pageable);

    /**
     * Confirmadas, en cola y sin revisar de una actividad, en una sola
     * consulta · {@code B3-03}.
     */
    @Query("""
            select new com.verisure.backend.repository.projection.RegistrationCounts(
                coalesce(sum(case when r.status = com.verisure.backend.entity.enums.RegistrationStatus.CONFIRMED
                                  then 1L else 0L end), 0L),
                coalesce(sum(case when r.status = com.verisure.backend.entity.enums.RegistrationStatus.WAITLISTED
                                  then 1L else 0L end), 0L),
                coalesce(sum(case when r.status = com.verisure.backend.entity.enums.RegistrationStatus.WAITLISTED
                                   and r.accepted = false
                                  then 1L else 0L end), 0L))
            from Registration r
            where r.activity.id = :activityId
            """)
    RegistrationCounts findCountsByActivityId(@Param("activityId") Long activityId);

}
