package com.verisure.backend.service;

import com.verisure.backend.dto.closure.ClosureDetailResponse;
import com.verisure.backend.dto.closure.CreateClosureRequest;

/**
 * El cierre que rellena el empleado: uno por inscripción.
 *
 * <p>Está separado de {@link ActivityClosureService} a propósito. El glosario
 * tiene dos términos —«cierre de participación» y «cierre de actividad»— y un
 * único {@code ClosureService} sería el único sitio del proyecto donde vuelve a
 * existir el «cierre» a secas que el glosario evita.
 *
 * <p><b>Este dominio no manda ningún correo.</b> Si algún día su controlador
 * necesita inyectar {@link NotificationService}, es señal de que se ha colado
 * un aviso donde no toca.
 *
 * <p>Tarea: B1-03.
 */
public interface ParticipationClosureService {

    /**
     * Crea el cierre de una inscripción. Solo puede existir uno por inscripción.
     *
     * @throws com.verisure.backend.exception.DomainException
     *         {@code ACTIVITY_NOT_FINISHED} si la actividad no ha terminado,
     *         {@code REGISTRATION_NOT_CONFIRMED} si la inscripción no estaba confirmada,
     *         {@code CLOSURE_ALREADY_CLOSED} si se corrige con la actividad ya cerrada.
     */
    ClosureDetailResponse submit(CreateClosureRequest request);

    /** Detalle de un cierre. {@code NOT_OWNER} si no es de quien lo pide y no es administración. */
    ClosureDetailResponse getById(Long closureId);
}
