package com.verisure.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.repository.projection.ActivityClosureAggregates;
import com.verisure.backend.repository.projection.ClosedParticipationView;

public interface ParticipationClosureRepository extends JpaRepository<ParticipationClosure, Long> {

    Optional<ParticipationClosure> findByRegistrationId(Long registrationId);

    /**
     * Agregado de participaciones cerradas para el dashboard de la Fundación.
     *
     * <p>Lleva {@code @Query} porque el nombre no es derivable y porque
     * {@link ClosedParticipationView} es un {@code record}, no una interfaz de
     * proyección: hace falta una expresión de constructor. El {@code cast} de
     * {@code organization} es necesario porque en la entidad es un enumerado y
     * el {@code record} espera un {@code String}.
     *
     * <p>Ambos filtros son opcionales: si llegan a {@code null} no filtran.
     */
    @Query("""
            select new com.verisure.backend.repository.projection.ClosedParticipationView(
                u.department, cast(u.organization as string), a.line, pc.actualHours, a.endDate)
            from ParticipationClosure pc
              join pc.registration r
              join r.user u
              join r.activity a
            where r.status = com.verisure.backend.entity.enums.RegistrationStatus.CLOSED
              and (:year is null or year(a.endDate) = :year)
              and (:line is null or a.line = :line)
            """)
    List<ClosedParticipationView> findClosedForDashboard(@Param("year") Integer year,
                                                         @Param("line") String line);

    /**
     * Totales de participación de una actividad, para la pantalla de cierre de
     * la Fundación.
     *
     * <p>Va aquí y no en un servicio para que {@code ActivityClosureService} no
     * tenga que llamar a {@code ParticipationClosureService}: los dos cierres
     * son dominios separados y solo comparten datos, no lógica.
     *
     * <p>El {@code coalesce} evita devolver {@code null} cuando todavía no ha
     * cerrado nadie, que es el estado normal de una actividad recién terminada.
     */
    @Query("""
            select new com.verisure.backend.repository.projection.ActivityClosureAggregates(
                coalesce(sum(pc.actualHours), 0L),
                count(pc),
                coalesce(sum(case when pc.evidenceUrl is not null then 1L else 0L end), 0L))
            from ParticipationClosure pc
              join pc.registration r
            where r.activity.id = :activityId
            """)
    ActivityClosureAggregates findAggregatesByActivityId(@Param("activityId") Long activityId);

}
