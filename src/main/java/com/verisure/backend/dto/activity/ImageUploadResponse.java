package com.verisure.backend.dto.activity;

/**
 * Respuesta de la subida de la portada de una actividad.
 *
 * <p>La URL es relativa a la raíz (p. ej. {@code /uploads/portadas/...}) y es
 * exactamente la que {@link CreateActivityRequest#imageUrl()} espera.
 *
 * <p>Dueña: BE2 · Tarea: B2-03.
 */
public record ImageUploadResponse(String url) {
}