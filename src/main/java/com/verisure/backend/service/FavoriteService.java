package com.verisure.backend.service;

/**
 * «Me gusta» de un empleado sobre una actividad.
 *
 * <p>Las dos operaciones son idempotentes, y por eso comprueban antes de
 * escribir: dejar que reviente la restricción única sube al manejador global
 * una excepción que sale como 500, y un doble clic en el corazón no es un error
 * del servidor.
 *
 * <p>Dueña: BE3 · Tarea: B3-07.
 */
public interface FavoriteService {

    /** Marca la actividad como favorita de quien llama. */
    void add(Long activityId, String userEmail);

    /** Quita el «me gusta» de quien llama sobre la actividad. */
    void remove(Long activityId, String userEmail);
}
