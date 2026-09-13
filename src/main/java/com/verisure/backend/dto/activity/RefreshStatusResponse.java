package com.verisure.backend.dto.activity;

/**
 * Cuántas actividades ha movido una pasada de la tarea de estados.
 *
 * <p>Los dos a cero es el resultado normal: solo hay transiciones el día en que
 * una actividad empieza o termina.
 *
 * <p>Dueña: BE3 · Tarea: B3-17.
 */
public record RefreshStatusResponse(int started, int finished) {
}
