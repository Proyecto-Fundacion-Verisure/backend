package com.verisure.backend.service;

import com.verisure.backend.entity.Registration;

/**
 * Todo lo que toca plazas y cola de una actividad.
 *
 * <p>Dueña: BE3 · Tarea: B3-02 · B3-05.
 */
public interface SpotService {

    /**
     * Solicita plaza: crea la inscripción siempre en WAITLISTED, al final de la cola.
     *
     * <p>No mira el cupo, porque solicitar no ocupa plaza: quien decide es la admin.
     * Comprueba primero el duplicado y después el plazo, en ese orden, para que el
     * error diga la razón real por la que no puede seguir.
     *
     * <p>Recibe el correo y no la entidad porque el filtro de seguridad deja en el
     * contexto el correo del token, no un {@code UserDetail}.
     */
    Registration register(Long activityId, String userEmail);

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

    /**
     * Asciende a la primera persona apta de la cola al liberarse una plaza, y
     * devuelve el identificador de su inscripción o {@code null} si no ascendió
     * nadie.
     *
     * <p>Quien llama es responsable de avisar con {@code notifySpotReleased}: el
     * destinatario nace dentro de esta transacción y su identificador no está en
     * ninguna ruta, así que el controlador no puede adivinarlo.
     */
    Long promoteFirstInQueue(Long activityId);

}
