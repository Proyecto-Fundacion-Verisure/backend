package com.verisure.backend.service;

import com.verisure.backend.dto.closure.CertificateResponse;

/**
 * Certificado de participación: {@code GET /api/closures/{id}/certificate}.
 *
 * <p>Tercero de los tres servicios de cierre de BE1, junto a
 * {@link ParticipationClosureService} y {@link ActivityClosureService}.
 *
 * <p>Tarea: B1-06.
 */
public interface CertificateService {

    /**
     * Genera el certificado de participación.
     *
     * @param closureId identificador del cierre de participación
     * @param userEmail correo de quien pide (el del token)
     * @throws com.verisure.backend.exception.NotFoundException si el cierre no existe,
     * @throws com.verisure.backend.exception.DomainException
     *         {@code NOT_OWNER} si el cierre no es de quien lo pide,
     *         {@code ACTIVITY_NOT_CLOSED} si la actividad aún no se ha cerrado.
     */
    CertificateResponse generate(Long closureId, String userEmail);
}
