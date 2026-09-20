package com.verisure.backend.dto.proposal;

import java.time.Instant;

import com.verisure.backend.entity.enums.ProposalStatus;

/**
 * La ficha de una propuesta: la fila de la bandeja más lo que hace falta para
 * decidirla y para poder contestar a quien la envió.
 *
 * <p>Los tres datos de contacto salen de la entidad, así que van vacíos en las
 * propuestas públicas que todavía no tienen una detrás.
 */
public record ProposalDetailResponse(
        Long id,
        String partnerName,
        String suggestedLine,
        int estimatedVolunteers,
        Integer scope,
        ProposalStatus status,
        Instant createdAt,
        Long activityId,
        String description,
        String contactName,
        String email,
        String phone,
        Instant consentAt) {
}
