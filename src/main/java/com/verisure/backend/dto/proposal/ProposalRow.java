package com.verisure.backend.dto.proposal;

import java.time.Instant;

import com.verisure.backend.entity.enums.ProposalStatus;

/**
 * Una fila de la bandeja de propuestas.
 *
 * <p>Lleva el estado porque es lo que decide el botón: nueva abre «Crear
 * actividad», aceptada lleva a la actividad que salió de ella y rechazada no
 * ofrece ninguna acción.
 *
 * <p>{@code partnerName} es nulo cuando la propuesta llegó por el formulario
 * público de una organización sin cuenta, y {@code activityId} solo tiene valor
 * en las aceptadas.
 *
 * <p>{@code scope} son personas beneficiarias, no ámbito geográfico.
 */
public record ProposalRow(
        Long id,
        String partnerName,
        String suggestedLine,
        int estimatedVolunteers,
        Integer scope,
        ProposalStatus status,
        Instant createdAt,
        Long activityId) {
}
