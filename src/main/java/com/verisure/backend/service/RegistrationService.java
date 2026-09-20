package com.verisure.backend.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.verisure.backend.dto.registration.CancelResult;
import com.verisure.backend.dto.registration.MyRegistrationItem;
import com.verisure.backend.dto.registration.RegistrationResponse;
import com.verisure.backend.entity.enums.RegistrationStatus;
import com.verisure.backend.repository.projection.RegistrationCounts;
import com.verisure.backend.repository.projection.RegistrationRow;

/**
 * Decisión de la administradora sobre una inscripción.
 *
 * <p>El controlador orquesta el aviso: llama a estos métodos y, si no
 * lanzan, manda el correo. Esta interfaz no toca NotificationService;
 * eso es cosa del controlador.
 *
 * <p>Las decisiones reciben el correo de la administradora y no su entidad,
 * porque el filtro de seguridad deja en el contexto el correo del token.
 *
 * <p>Dueña: BE3 · Tarea: B3-03 · B3-06.
 */
public interface RegistrationService {

    /**
     * Acepta una inscripción: la marca como apta ({@code accepted = true}).
     *
     * <p>Con hueco la confirma; sin hueco se queda {@code WAITLISTED} en su
     * posición. Nunca falla por aforo.
     */
    RegistrationResponse accept(Long registrationId, String adminEmail);

    /**
     * Rechaza una inscripción: la pasa a {@code REJECTED}.
     *
     * <p>Sin cuerpo y sin motivo. El rechazo es un estado, no una conversación.
     */
    RegistrationResponse reject(Long registrationId, String adminEmail);

    /**
     * Tablero de administración: inscripciones de una actividad, con las horas
     * del año en curso.
     *
     * <p>Los dos filtros son opcionales: a {@code null}, no aplican.
     */
    Page<RegistrationRow> getAdminDashboard(Long activityId,
                                            RegistrationStatus status,
                                            Pageable pageable);

    /** Confirmadas, en cola y sin revisar para el tablero; sin actividad, de todas. */
    RegistrationCounts getCounts(Long activityId);

    /**
     * Cancela una inscripción, la pida su dueña o la administradora.
     *
     * <p>La ruta sirve a dos roles y la cadena de seguridad solo exige token,
     * así que el permiso se comprueba aquí: la dueña solo hasta la fecha
     * de inicio, la administradora siempre, y cualquier otra persona
     * {@code NOT_OWNER}.
     *
     * <p>El motivo no se guarda en ningún sitio: se registra en la traza.
     */
    CancelResult cancel(Long registrationId, String callerEmail, String reason);

    /** «Mis voluntariados»: las inscripciones de quien llama, con su cierre. */
    List<MyRegistrationItem> findMine(String userEmail);
}
