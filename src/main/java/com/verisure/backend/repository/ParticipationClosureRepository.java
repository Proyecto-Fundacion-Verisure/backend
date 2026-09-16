package com.verisure.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.entity.ParticipationClosure;
import com.verisure.backend.repository.projection.ActivityClosureAggregates;
import com.verisure.backend.repository.projection.DashboardClosedRow;
import com.verisure.backend.repository.projection.PartnerDashboardRow;

public interface ParticipationClosureRepository extends JpaRepository<ParticipationClosure, Long> {

    Optional<ParticipationClosure> findByRegistrationId(Long registrationId);

    /**
     * Los cierres de participación de una actividad, para repartir la referencia
     * del certificado al cerrar la actividad · B1-21.
     */
    List<ParticipationClosure> findByRegistration_ActivityId(Long activityId);

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

    /**
     * Las participaciones cerradas con todo lo que el dashboard necesita ·
     * {@code B1-07}.
     *
     * <p>Es la consulta operativa del dashboard: {@code DashboardService} agrega
     * a partir de estas filas (horas, voluntarios distintos, actividades, entidades,
     * distribuciones por departamento, modalidad y ubicación). {@code DashboardClosedRow}
     * es un {@code record}, por eso la expresión de constructor, y el {@code left join}
     * a {@code partner} es obligatorio porque la Fundación publica actividades sin
     * entidad detrás.
     */
    @Query("""
            select new com.verisure.backend.repository.projection.DashboardClosedRow(
                pc.id, a.id, a.title, r.user.id, u.department,
                cast(u.organization as string), a.line, a.mode, a.location,
                pc.actualHours, a.endDate, p.id, p.name)
            from ParticipationClosure pc
              join pc.registration r
              join r.user u
              join r.activity a
              left join a.partner p
            where r.status = com.verisure.backend.entity.enums.RegistrationStatus.CLOSED
              and (:year is null or year(a.endDate) = :year)
              and (:line is null or a.line = :line)
            order by a.endDate
            """)
    List<DashboardClosedRow> findDashboardData(@Param("year") Integer year,
                                               @Param("line") String line);

    /**
     * Entidades con participación cerrada para {@code partners.csv} · {@code B1-08}.
     *
     * <p>Va {@code group by} a nivel de entidad y no de participación: la fila es
     * la entidad, con cuántas actividades distintas aportó y cuántas horas suman.
     * El {@code join} interno a {@code partner} excluye las actividades que la
     * Fundación publica por su cuenta, que no tienen a quien atribuirlas.
     */
    @Query("""
            select new com.verisure.backend.repository.projection.PartnerDashboardRow(
                p.id, p.name, count(distinct a.id), sum(pc.actualHours))
            from ParticipationClosure pc
              join pc.registration r
              join r.activity a
              join a.partner p
            where r.status = com.verisure.backend.entity.enums.RegistrationStatus.CLOSED
              and (:year is null or year(a.endDate) = :year)
            group by p.id, p.name
            order by sum(pc.actualHours) desc
            """)
    List<PartnerDashboardRow> findPartnersForDashboard(@Param("year") Integer year);

}
