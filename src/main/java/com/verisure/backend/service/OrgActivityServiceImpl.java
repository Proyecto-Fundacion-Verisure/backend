package com.verisure.backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.dto.activity.CreateActivityRequest;
import com.verisure.backend.dto.org.OrgActivityRow;
import com.verisure.backend.entity.Activity;
import com.verisure.backend.entity.Partner;
import com.verisure.backend.entity.enums.ActivityStatus;
import com.verisure.backend.exception.DomainException;
import com.verisure.backend.exception.ErrorCode;
import com.verisure.backend.exception.NotFoundException;
import com.verisure.backend.repository.ActivityRepository;
import com.verisure.backend.repository.RegistrationRepository;
import com.verisure.backend.repository.projection.ActivitySpotCount;

import lombok.RequiredArgsConstructor;

/**
 * Listar, crear, editar y enviar a revisión las actividades de una entidad.
 *
 * <p>Dueña: BE3 · Tareas: B2-13 y B2-14.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgActivityServiceImpl implements OrgActivityService {

    private final ActivityRepository activityRepository;
    private final RegistrationRepository registrationRepository;
    private final PartnerResolver partnerResolver;

    /** {@inheritDoc} */
    @Override
    public Page<OrgActivityRow> list(
            ActivityStatus status, Pageable pageable, String userEmail) {

        Long partnerId = partnerResolver.resolve(userEmail).getId();

        boolean filteredByStatus = status != null;
        Page<Activity> page;
        if (filteredByStatus) {
            page = activityRepository.findByPartnerIdAndStatus(partnerId, status, pageable);
        } else {
            page = activityRepository.findByPartnerId(partnerId, pageable);
        }

        // Un recuento para la página entera y no uno por fila.
        List<Long> activityIds = page.getContent().stream().map(Activity::getId).toList();
        Map<Long, Integer> occupiedSpots = countOccupiedSpots(activityIds);

        return page.map(activity -> toRow(activity, occupiedSpots));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public OrgActivityRow create(CreateActivityRequest request, String userEmail) {
        Partner partner = partnerResolver.resolve(userEmail);

        Activity activity = new Activity();
        applyTo(request, activity);
        activity.setPartner(partner);
        // La entidad no fija el estado: de DRAFT solo la mueve submit.
        activity.setStatus(ActivityStatus.DRAFT);

        // Sin recuento: la actividad acaba de nacer y no hay nadie dentro.
        return toRow(activityRepository.save(activity), Map.of());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public OrgActivityRow update(Long activityId, CreateActivityRequest request, String userEmail) {
        Activity activity = findOwnEditableOrFail(activityId, userEmail);

        applyTo(request, activity);

        return toRow(activity, countOccupiedSpots(List.of(activityId)));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public OrgActivityRow submit(Long activityId, String userEmail) {
        Activity activity = findOwnEditableOrFail(activityId, userEmail);

        // Sin comprobar campos obligatorios: CreateActivityRequest ya los exige
        // todos, así que no hay borradores incompletos que rechazar.
        activity.setStatus(ActivityStatus.PENDING_APPROVAL);

        return toRow(activity, countOccupiedSpots(List.of(activityId)));
    }

    /**
     * La actividad, comprobando que es de quien llama y que sigue en borrador.
     *
     * <p>La de otra entidad da 403 y no 404: el recurso existe y tiene un dueño
     * identificado, al contrario que en la ficha del catálogo.
     */
    private Activity findOwnEditableOrFail(Long activityId, String userEmail) {
        Long partnerId = partnerResolver.resolve(userEmail).getId();

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> NotFoundException.of("actividad", activityId));

        Partner owner = activity.getPartner();
        boolean fromAnotherPartner = owner == null || !owner.getId().equals(partnerId);
        if (fromAnotherPartner) {
            throw new DomainException(ErrorCode.NOT_OWNER);
        }

        // Desde PENDING_APPROVAL la entidad ya no edita: está esperando decisión.
        boolean locked = activity.getStatus() != ActivityStatus.DRAFT;
        if (locked) {
            throw new DomainException(ErrorCode.ACTIVITY_NOT_EDITABLE);
        }

        return activity;
    }

    /** Copia lo que la entidad puede escribir. Ni el estado ni la entidad salen de aquí. */
    private void applyTo(CreateActivityRequest request, Activity activity) {
        activity.setTitle(request.title());
        activity.setDescription(request.description());
        activity.setLine(request.line());
        activity.setMode(request.mode());
        activity.setLocation(request.location());
        activity.setStartDate(request.startDate());
        activity.setEndDate(request.endDate());
        activity.setRegistrationDeadline(request.registrationDeadline());
        activity.setHours(request.hours());
        activity.setSpots(request.spots());
    }

    /**
     * Plazas cubiertas, con cero en las que no tienen ninguna.
     *
     * <p>Misma consulta que el catálogo, para que «plaza ocupada» signifique lo
     * mismo en los dos sitios.
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

    private OrgActivityRow toRow(Activity activity, Map<Long, Integer> occupiedSpots) {
        return new OrgActivityRow(
                activity.getId(),
                activity.getTitle(),
                activity.getLine(),
                activity.getMode(),
                activity.getLocation(),
                activity.getStartDate(),
                activity.getEndDate(),
                activity.getRegistrationDeadline(),
                activity.getHours(),
                activity.getSpots(),
                occupiedSpots.getOrDefault(activity.getId(), 0),
                activity.getStatus(),
                activity.getReviewNote());
    }
}
