package com.verisure.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.verisure.backend.entity.ActivityClosure;

public interface ActivityClosureRepository extends JpaRepository<ActivityClosure, Long>{

    /**
     * El cierre de una actividad; vacío si aún no existe.
     *
     * <p>Derivable: la relación {@code activity} es {@code @OneToOne} y la
     * columna {@code activity_id} es única. Tarea: B1-04.
     */
    Optional<ActivityClosure> findByActivityId(Long activityId);

}
