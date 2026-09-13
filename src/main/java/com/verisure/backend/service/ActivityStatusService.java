package com.verisure.backend.service;

import java.util.List;

/**
 * Paso del tiempo: lleva cada actividad al estado que le toca por sus fechas.
 *
 * <p>Es el único sitio del proyecto que escribe {@code IN_PROGRESS} y
 * {@code FINISHED}. Todo el ciclo de cierre de BE1 arranca en ese segundo
 * estado, así que sin estos dos métodos no se puede cerrar nada.
 *
 * <p>Dueña: BE3 · Tarea: B3-17.
 */
public interface ActivityStatusService {

    /** Pasa a IN_PROGRESS las publicadas y llenas que ya han empezado. */
    int startDueActivities();

    /**
     * Pasa a FINISHED las que ya han terminado, con sus inscripciones
     * confirmadas a PENDING_CLOSURE, y devuelve los identificadores.
     *
     * <p>Devuelve la lista porque el aviso se manda fuera de esta transacción:
     * quien llama avisa cuando este método ha vuelto sin lanzar.
     */
    List<Long> finishDueActivities();

}
