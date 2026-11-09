package com.verisure.backend.service;

import com.verisure.backend.dto.org.OrgDashboardResponse;

/**
 * Panel de impacto de una entidad · {@code B2-15}.
 *
 * <p>Dueña: BE3. La entidad sale siempre de la sesión, nunca de un parámetro.
 */
public interface OrgDashboardService {

    /**
     * @param year     año de {@code activity.endDate} por el que filtrar, o
     *                 {@code null} para todos
     * @param userEmail correo de la cuenta autenticada
     */
    OrgDashboardResponse getDashboard(Integer year, String userEmail);
}
