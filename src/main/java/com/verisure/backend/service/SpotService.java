package com.verisure.backend.service;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.User;

/**
 * Todo lo que toca plazas y cola de una actividad.
 *
 * <p>Dueña: BE3 · Tarea: B3-02.
 */
public interface SpotService {

    /**
     * Solicita plaza: crea la inscripción siempre en WAITLISTED, al final de la cola.
     *
     * <p>No mira el cupo, porque solicitar no ocupa plaza: quien decide es la admin.
     * Comprueba primero el duplicado y después el plazo, en ese orden, para que el
     * error diga la razón real por la que no puede seguir.
     */
    Registration register(Long activityId, User user);

    /** ¿Quedan plazas? Cupo total menos confirmadas; las de la cola no ocupan. */
    boolean hasFreeSpot(Long activityId);

    /**
     * Sincroniza el estado de la actividad con las plazas cubiertas, en las dos
     * direcciones: PUBLISHED ↔ FULL.
     *
     * <p>La dispara la confirmación, no la solicitud. Y escribe en Activity, de
     * BE2: el reparto asigna FULL a B3-02.
     */
    void refreshFullStatus(Long activityId);

}
