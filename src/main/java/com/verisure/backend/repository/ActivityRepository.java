package com.verisure.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.entity.Activity;
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

}
