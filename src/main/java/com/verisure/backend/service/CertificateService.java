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
     * @throws com.verisure.backend.exception.DomainException
     *         {@code ACTIVITY_NOT_CLOSED} si la actividad aún no se ha cerrado,
     *         {@code NOT_OWNER} si el cierre no es de quien lo pide.
     */
    CertificateResponse generate(Long closureId);
}
