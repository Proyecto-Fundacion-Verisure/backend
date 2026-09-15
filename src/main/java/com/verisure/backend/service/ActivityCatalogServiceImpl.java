package com.verisure.backend.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.activity.ActivityCardResponse;
import com.verisure.backend.dto.activity.ActivityDetailResponse;
import com.verisure.backend.dto.activity.CatalogFilters;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.User;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.FavoriteRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.UserRepository;
import com.verisure.backend.repository.projection.ActivityCatalogRow;
import com.verisure.backend.repository.projection.ActivitySpotCount;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityCatalogServiceImpl implements ActivityCatalogService {

    /**
     * Los cuatro estados que el catálogo enseña.
     *
     * <p>{@code FULL} está dentro a propósito: es un estado distinto de
     * {@code PUBLISHED}, y dejarlo fuera cierra la ficha justo cuando la
     * actividad se llena, que es cuando más gente entra a apuntarse a la cola.
     */
    private static final List<ActivityStatus> VISIBLE_STATUSES = List.of(
            ActivityStatus.PUBLISHED,
            ActivityStatus.FULL,
            ActivityStatus.IN_PROGRESS,
            ActivityStatus.FINISHED);

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    /** {@inheritDoc} */
    @Override
    public Page<ActivityCardResponse> list(
            CatalogFilters filters, Pageable pageable, String userEmail) {

        Page<ActivityCatalogRow> page = activityRepository.findCatalog(
                VISIBLE_STATUSES,
                filters.line(), filters.mode(), filters.from(), filters.to(),
                pageable);

        // Dos consultas para la página entera y no dos por tarjeta: es lo que
        // deja el catálogo en tres sentencias, se pinten las tarjetas que se pinten.
        List<Long> activityIds = page.getContent().stream()
                .map(ActivityCatalogRow::id)
                .toList();
        Map<Long, Integer> occupiedSpots = countOccupiedSpots(activityIds);
        Set<Long> favorited = findFavorited(activityIds, userEmail);

        return page.map(row -> toCard(row, occupiedSpots, favorited));
    }

    /** {@inheritDoc} */
    @Override
    public ActivityDetailResponse detail(Long activityId, String userEmail) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> NotFoundException.of("actividad", activityId));

        boolean hidden = !VISIBLE_STATUSES.contains(activity.getStatus());
        if (hidden) {
            throw NotFoundException.of("actividad", activityId);
        }

        // Las plazas ocupadas se cuentan con la misma consulta que la lista: dos
        // definiciones de «plaza ocupada» enseñarían números distintos en la
        // rejilla y en la ficha de la misma actividad.
        Map<Long, Integer> occupiedSpots = countOccupiedSpots(List.of(activityId));
        int occupied = occupiedSpots.getOrDefault(activityId, 0);

        User user = findUserOrFail(userEmail);
        boolean favorited = favoriteRepository.existsByActivityIdAndUserId(
                activityId, user.getId());

        return toDetail(activity, occupied, favorited);
    }

    /**
     * Plazas cubiertas de la página, con cero en las que no tienen ninguna.
     *
     * <p>La consulta agrupa, así que las actividades sin nadie dentro no vuelven
     * en el resultado; el cero lo pone quien lee el mapa.
     */
    private Map<Long, Integer> countOccupiedSpots(List<Long> activityIds) {
        if (activityIds.isEmpty()) {
            return Map.of();
        }
        return registrationRepository.countOccupiedSpotsByActivityIds(activityIds).stream()
                .collect(Collectors.toMap(
                        ActivitySpotCount::activityId,
                        count -> count.occupied().intValue()));
    }

    /**
     * De la página, cuáles ha marcado quien mira.
     *
     * <p>Para la administradora sale siempre vacío, y es correcto: no tiene
     * corazón en el catálogo.
     */
    private Set<Long> findFavorited(List<Long> activityIds, String userEmail) {
        if (activityIds.isEmpty()) {
            return Set.of();
        }
        User user = findUserOrFail(userEmail);
        return Set.copyOf(
                favoriteRepository.findFavoritedActivityIds(user.getId(), activityIds));
    }

    private ActivityDetailResponse toDetail(
            Activity activity,
            int occupiedSpots,
            boolean favoritedByMe) {

        Partner partner = activity.getPartner();
        String partnerName = partner != null ? partner.getName() : null;

        return new ActivityDetailResponse(
                activity.getId(),
                activity.getTitle(),
                partnerName,
                activity.getLine(),
                activity.getMode(),
                activity.getLocation(),
                activity.getDescription(),
                activity.getStartDate(),
                activity.getEndDate(),
                activity.getRegistrationDeadline(),
                activity.getHours(),
                activity.getSpots(),
                occupiedSpots,
                activity.getStatus(),
                favoritedByMe);
    }

    private ActivityCardResponse toCard(
            ActivityCatalogRow row,
            Map<Long, Integer> occupiedSpots,
            Set<Long> favorited) {

        return new ActivityCardResponse(
                row.id(),
                row.title(),
                row.partnerName(),
                row.line(),
                row.mode(),
                row.location(),
                row.startDate(),
                row.endDate(),
                row.hours(),
                row.spots(),
                occupiedSpots.getOrDefault(row.id(), 0),
                row.status(),
                favorited.contains(row.id()));
    }

    private User findUserOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("No existe la cuenta de " + email));
    }
}
