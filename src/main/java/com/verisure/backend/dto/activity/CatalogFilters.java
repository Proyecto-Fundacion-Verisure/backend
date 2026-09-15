package com.verisure.backend.dto.activity;

import java.time.LocalDate;

/**
 * Los cuatro filtros del catálogo, todos opcionales.
 *
 * <p>Van juntos en un tipo y no sueltos en la firma porque son dos textos
 * seguidos y dos fechas seguidas: confundir {@code from} con {@code to}
 * compilaría igual y fallaría en silencio.
 *
 * <p>{@code from} y {@code to} acotan la fecha de inicio, extremos incluidos.
 */
public record CatalogFilters(
        String line,
        String mode,
        LocalDate from,
        LocalDate to) {
}
