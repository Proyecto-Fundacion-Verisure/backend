package com.verisure.backend.dto.activity;

import java.time.LocalDate;

import com.verisure.backend.entity.enums.ActivityStatus;

/**
 * Lo que carga el formulario de edición de la administración.
 *
 * <p>Devuelve <b>todos</b> los campos que el formulario escribe, sin recuento de
 * plazas ni corazón: son cosas del catálogo, no del editor. Funciona con
 * <b>cualquier</b> estado, incluidos {@code DRAFT} y {@code CANCELLED}, porque
 * la administradora tiene que poder reabrir una actividad aunque no sea visible
 * en el catálogo. Es la diferencia con {@link ActivityDetailResponse}, que solo
 * sirve los estados visibles.
 *
 * <p>No lleva {@code imageUrl}: la portada es la imagen por defecto de la línea
 * y la resuelve el frontend · B2-03.
 */
public record ActivityFormResponse(
    Long id,
    String title,
    String description,
    String line,
    String mode,
    String location,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate registrationDeadline,
    Integer hours,
    Integer spots,
    ActivityStatus status,
    String partnerName) {

}