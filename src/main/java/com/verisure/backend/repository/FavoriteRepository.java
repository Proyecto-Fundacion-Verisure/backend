package com.verisure.backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.verisure.backend.dto.dashboard.FavoriteRankingEntry;
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

    /**
     * Las diez actividades con más «me gusta» entre las que tienen participación
     * cerrada · {@code B1-07}.
     *
     * <p>La demanda se mide sobre las actividades que de verdad se llevaron a
     * cabo y cuentan en el dashboard; una actividad que nadie llegó a realizar
     * no aparece en el ranking aunque acumule corazones. La página la limita el
     * servicio con {@code PageRequest.of(0, 10)}.
     */
    @Query("""
            select new com.verisure.backend.dto.dashboard.FavoriteRankingEntry(
                f.activity.id, f.activity.title, count(f))
            from Favorite f
            where f.activity.id in (
                select distinct a.id
                from ParticipationClosure pc
                  join pc.registration r
                  join r.activity a
                where r.status = com.verisure.backend.entity.enums.RegistrationStatus.CLOSED
                  and (:year is null or year(a.endDate) = :year)
                  and (:line is null or a.line = :line))
            group by f.activity.id, f.activity.title
            order by count(f) desc
            """)
    List<FavoriteRankingEntry> findTopRanking(
            @Param("year") Integer year,
            @Param("line") String line,
            Pageable pageable);
}
