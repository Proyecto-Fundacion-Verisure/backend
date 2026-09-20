package com.verisure.backend.dto.org;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que la entidad escribe al proponer desde dentro.
 *
 * <p>Sin CIF ni contacto: la entidad se resuelve desde el token.
 * {@code scope} son personas beneficiarias, no ámbito geográfico.
 *
 * <p>Dueña: BE3 · Tarea: B2-16.
 */
public record CreateOrgProposalRequest(
        @NotBlank String description,
        @Size(max = 60) String suggestedLine,
        @NotNull @Min(1) Integer estimatedVolunteers,
        @Min(1) Integer scope) {
}
