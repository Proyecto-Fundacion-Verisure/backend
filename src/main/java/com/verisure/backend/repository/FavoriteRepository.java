package com.verisure.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.entity.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByActivityIdAndUserId(Long activityId, Long userId);

    long deleteByActivityIdAndUserId(Long activityId, Long userId);

    /**
     * De estas actividades, cuáles ha marcado esta persona · {@code B2-07}.
     *
     * <p>Va acotada a los identificadores de la página y no a todos los favoritos
     * de la cuenta, para que la consulta no crezca con los años de uso.
     */
    @Query("""
            select f.activity.id
            from Favorite f
            where f.user.id = :userId
              and f.activity.id in :activityIds
            """)
    List<Long> findFavoritedActivityIds(
            @Param("userId") Long userId,
            @Param("activityIds") List<Long> activityIds);
}
