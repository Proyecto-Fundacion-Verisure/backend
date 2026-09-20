package com.verisure.backend.dto.dashboard;

/**
 * Participantes por departamento, para el gráfico de barras del dashboard.
 */
public record DepartmentEntry(String department, long participants) {
}