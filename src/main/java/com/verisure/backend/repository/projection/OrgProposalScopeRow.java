package com.verisure.backend.repository.projection;

import java.time.LocalDate;

/**
 * Personas beneficiarias declaradas por una entidad · {@code B2-15}.
 *
 * <p>Una fila por propuesta aceptada: el alcance que declaró la entidad y el
 * día en que terminó la actividad que salió de ella. Con eso se puede sumar el
 * año pedido y comparar trimestres sin volver a consultar.
 */
public record OrgProposalScopeRow(Integer scope, LocalDate endDate) {
}
