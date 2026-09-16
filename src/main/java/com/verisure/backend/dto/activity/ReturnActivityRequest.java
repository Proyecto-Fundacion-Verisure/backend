package com.verisure.backend.dto.activity;

import jakarta.validation.constraints.NotBlank;

/**
 * Comentario con el que se devuelve una actividad propuesta · {@code B2-15}.
 *
 * <p>El campo se llama {@code note} por contrato con el frontend, que lo manda
 * así; el servicio lo guarda después en {@code Activity.reviewNote}.
 */
public record ReturnActivityRequest(@NotBlank String note) {

}