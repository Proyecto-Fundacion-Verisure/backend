package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.activity.ActivityCardResponse;
import com.verisure.backend.dto.activity.ActivityDetailResponse;
import com.verisure.backend.dto.activity.CatalogFilters;

/**
 * El catálogo que ve el empleado: la rejilla y la ficha.
 *
 * <p>Va aparte de {@link ActivityService}, que es la administración de
 * actividades, porque son dos lecturas distintas: aquí solo se ven los cuatro
 * estados visibles, y cada respuesta lleva el corazón de quien mira.
 *
 * <p>Dueña: BE3 · Tarea: B2-07.
 */
public interface ActivityCatalogService {

    /**
     * Una página del catálogo.
     *
     * <p>Recibe el correo y no la entidad porque el filtro de seguridad deja en
     * el contexto el correo del token, igual que en el resto de servicios.
     */
    Page<ActivityCardResponse> list(
            CatalogFilters filters, Pageable pageable, String userEmail);

    /**
     * La ficha de una actividad.
     *
     * <p>En un estado que no sea visible lanza 404 y no 403: quien no debe
     * verla no debe ni saber que existe.
     */
    ActivityDetailResponse detail(Long activityId, String userEmail);
}
