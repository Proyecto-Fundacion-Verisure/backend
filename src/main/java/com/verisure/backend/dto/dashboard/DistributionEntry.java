package com.verisure.backend.dto.dashboard;

/**
 * Una porción de una distribución porcentual (modalidad o ubicación).
 *
 * <p>{@code value} es el porcentaje sobre el total de participaciones cerradas.
 */
public record DistributionEntry(String id, String label, double value) {
}