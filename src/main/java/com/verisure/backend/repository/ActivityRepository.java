package com.verisure.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.repository.projection.ActivityCatalogRow;
import com.verisure.backend.repository.projection.ActivityMailView;
import com.verisure.backend.repository.projection.ActivitySummary;
import com.verisure.backend.repository.projection.SpotInfo;

public interface ActivityRepository extends JpaRepository<Activity, Long>{

    /**
     * Cupo, inscripciones confirmadas y fecha límite de una actividad.
     *
     * <p>Es la fuente de {@code ActivityQueryService.getSpotInfo}, la firma que
     * BE2 publica para que BE3 no tenga que leer {@code Activity} directamente.
     * {@link SpotInfo} es un {@code record}, así que la consulta usa una
     * expresión de constructor.
     */
    @Query("""
            select new com.verisure.backend.repository.projection.SpotInfo(
                a.spots,
                (select count(r) from Registration r
                  where r.activity = a
                    and r.status = com.verisure.backend.entity.enums.RegistrationStatus.CONFIRMED),
                a.registrationDeadline)
            from Activity a
            where a.id = :activityId
            """)
    Optional<SpotInfo> findSpotInfo(@Param("activityId") Long activityId);

    /**
     * La actividad con su fila bloqueada hasta que confirme la transacción.
     *
     * <p>La usa {@code SpotService.promoteFirstInQueue} · {@code B3-05}: sin el
     * bloqueo, dos bajas simultáneas leen el mismo cupo libre y ascienden a dos
     * personas a la misma plaza, sin que salte ningún error.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activity a where a.id = :activityId")
    Optional<Activity> findByIdForUpdate(@Param("activityId") Long activityId);

    /**
     * Las actividades que ya deberían haber empezado · {@code B3-17}.
     *
     * <p>El día de inicio ya cuenta como empezada, por eso {@code <=}.
     */
    List<Activity> findByStatusInAndStartDateLessThanEqual(
            List<ActivityStatus> statuses, LocalDate date);

    /**
     * Las actividades que ya deberían haber terminado · {@code B3-17}.
     *
     * <p>El día de fin todavía cuenta como en curso, por eso {@code <} y no
     * {@code <=}: una actividad no termina hasta que ese día pasa.
     */
    List<Activity> findByStatusAndEndDateBefore(ActivityStatus status, LocalDate date);

    @Query(value = """
            select new com.verisure.backend.repository.projection.ActivitySummary(
                a.id, a.title, a.partner.name, a.status,
                a.startDate, a.endDate, a.spots,
                count(f))
            from Activity a
            left join a.favorites f
            where (:status is null or a.status = :status)
            group by a.id, a.title, a.partner.name, a.status,
                     a.startDate, a.endDate, a.spots
            """,
            countQuery = """
            select count(a.id) from Activity a
            where (:status is null or a.status = :status)
            """)
    Page<ActivitySummary> findByStatusWithFavoriteCount(
            @Param("status") ActivityStatus status, Pageable pageable);

    @Query(value = """
            select new com.verisure.backend.repository.projection.ActivitySummary(
                a.id, a.title, a.partner.name, a.status,
                a.startDate, a.endDate, a.spots,
                count(f))
            from Activity a
            left join a.favorites f
            where (:term is null or :term = ''
                or lower(a.title) like lower(concat('%', :term, '%'))
                or lower(a.partner.name) like lower(concat('%', :term, '%')))
            group by a.id, a.title, a.partner.name, a.status,
                     a.startDate, a.endDate, a.spots
            """,
            countQuery = """
            select count(a.id) from Activity a
            where (:term is null or :term = ''
                or lower(a.title) like lower(concat('%', :term, '%'))
                or lower(a.partner.name) like lower(concat('%', :term, '%')))
            """)
    Page<ActivitySummary> searchByTitleOrPartner(
            @Param("term") String term, Pageable pageable);

    /**
     * Datos de una actividad para los correos del rol entidad · {@code B3-09}.
     *
     * <p>El {@code coalesce} del correo cae en la entidad cuando la actividad
     * la creó administración y no hay cuenta creadora: sin él ese aviso no
     * llegaría a nadie.
     */
    @Query("""
            select new com.verisure.backend.repository.projection.ActivityMailView(
                a.id, a.title, a.reviewNote, p.name,
                coalesce(u.fullName, p.contactName),
                coalesce(u.email, p.email))
            from Activity a
            left join a.partner p
            left join a.createdBy u
            where a.id = :activityId
            """)
    Optional<ActivityMailView> findMailViewById(@Param("activityId") Long activityId);

    /**
     * Una página del catálogo · {@code B2-07}.
     *
     * <p>La entidad va en {@code left join} y no implícita por {@code a.partner.name}:
     * una actividad que publica la Fundación por su cuenta no tiene entidad detrás,
     * y con un {@code join} interno desaparecería del catálogo. {@code from} y
     * {@code to} acotan la fecha de inicio, extremos incluidos.
     */
    @Query(value = """
            select new com.verisure.backend.repository.projection.ActivityCatalogRow(
                a.id, a.title, p.name, a.line, a.mode, a.location,
                a.startDate, a.endDate, a.hours, a.spots, a.imageUrl, a.status)
            from Activity a
            left join a.partner p
            where a.status in :statuses
              and (:line is null or a.line = :line)
              and (:mode is null or a.mode = :mode)
              and (cast(:from as localdate) is null or a.startDate >= :from)
              and (cast(:to as localdate) is null or a.startDate <= :to)
            """,
            countQuery = """
            select count(a.id)
            from Activity a
            where a.status in :statuses
              and (:line is null or a.line = :line)
              and (:mode is null or a.mode = :mode)
              and (cast(:from as localdate) is null or a.startDate >= :from)
              and (cast(:to as localdate) is null or a.startDate <= :to)
            """)
    Page<ActivityCatalogRow> findCatalog(
            @Param("statuses") List<ActivityStatus> statuses,
            @Param("line") String line,
            @Param("mode") String mode,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

}
