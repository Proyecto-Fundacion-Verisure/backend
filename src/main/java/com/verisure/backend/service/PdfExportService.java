package com.verisure.backend.service;

/**
 * Informe PDF del dashboard · {@code B1-10}.
 *
 * <p>Es la pieza que el plan recorta primero si falta tiempo: no rompe ningún
 * recorrido si no está. Reutiliza los mismos agregados que {@code GET
 * /api/dashboard} y los pinta en páginas A4.
 */
public interface PdfExportService {

    byte[] exportDashboardPdf(Integer year, String line);
}