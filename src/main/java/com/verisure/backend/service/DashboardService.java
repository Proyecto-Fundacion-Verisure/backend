package com.verisure.backend.service;

import com.verisure.backend.dto.dashboard.DashboardResponse;

/**
 * Los agregados del dashboard de la Fundación · {@code B1-07}.
 *
 * <p>Todo sale de experiencias de voluntariado finalizadas (participaciones
 * {@code CLOSED}); las actividades que no se llegaron a realizar no cuentan.
 * Los dos filtros, año y línea, son opcionales y se combinan con Y.
 */
public interface DashboardService {

    DashboardResponse getDashboard(Integer year, String line);
}