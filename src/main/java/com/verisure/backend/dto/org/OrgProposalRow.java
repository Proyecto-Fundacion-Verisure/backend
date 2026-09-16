package com.verisure.backend.dto.org;

import java.time.Instant;

import com.verisure.backend.entity.enums.ProposalStatus;

/**
 * Una propuesta vista por la entidad que la escribió.
 *
 * <p>Sin el nombre ni el contacto de la entidad, que ya son suyos, y sin datos
 * de personas · {@code B1-19}. {@code activityId} solo tiene valor en las
 * {@code ACCEPTED}.
 *
 * <p>Dueña: BE3 · Tarea: B2-16.
 */
public record OrgProposalRow(
        Long id,
        String description,
        String suggestedLine,
        Integer estimatedVolunteers,
        Integer scope,
        ProposalStatus status,
        Instant createdAt,
        Long activityId) {
}
