package com.verisure.backend.dto.proposal;

import java.time.Instant;

import com.verisure.backend.entity.enums.ProposalStatus;

/**
 * La ficha de una propuesta: la fila de la bandeja más lo que hace falta para
 * decidirla y para poder contestar a quien la envió.
 *
 * <p>El nombre, el CIF y el contacto salen de la entidad o, en las propuestas del
 * formulario público, de lo que escribió quien la envió.
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
        Instant consentAt,
        String cif) {
}
