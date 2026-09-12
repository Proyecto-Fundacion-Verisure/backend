package com.verisure.backend.repository;

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

}
