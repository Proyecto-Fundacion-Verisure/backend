package com.verisure.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.entity.Registration;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.projection.RegistrationCounts;
import com.verisure.backend.repository.projection.RegistrationRow;

/**
 * Decisión de la administradora sobre una inscripción.
 *
 * <p>El controlador orquesta el aviso: llama a estos métodos y, si no
 * lanzan, manda el correo. Esta interfaz <b>no toca NotificationService</b>;
 * eso es cosa del controlador.
 *
 * <p>Las decisiones reciben el correo de la administradora y no su entidad,
 * porque el filtro de seguridad deja en el contexto el correo del token.
 *
 * <p>Dueña: BE3 · Tarea: B3-03.
 */
public interface RegistrationService {

    /**
     * Acepta una inscripción: la marca como apta ({@code accepted = true}).
     *
     * <p>Con hueco la confirma; sin hueco se queda {@code WAITLISTED} en su
     * posición. <b>Nunca falla por aforo.</b>
     */
    Registration accept(Long registrationId, String adminEmail);

    /**
     * Rechaza una inscripción: la pasa a {@code REJECTED}.
     *
     * <p>Sin cuerpo y sin motivo. El rechazo es un estado, no una conversación.
     */
    Registration reject(Long registrationId, String adminEmail);

    /**
     * Tablero de administración: inscripciones de una actividad, con las horas
     * del año en curso.
     *
     * <p>Los dos filtros son opcionales: a {@code null}, no aplican.
     */
    Page<RegistrationRow> getAdminDashboard(Long activityId,
                                            RegistrationStatus status,
                                            Pageable pageable);

    /** Confirmadas, en cola y sin revisar de una actividad, para el tablero. */
    RegistrationCounts getCounts(Long activityId);
}
