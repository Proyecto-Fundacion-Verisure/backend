package com.verisure.backend.service;

/**
 * Las dos exportaciones CSV del dashboard · {@code B1-08}.
 *
 * <p>Ambas devuelven la cadena con BOM UTF-8 y punto y coma para que Excel
 * abra los acentos bien, y ninguna contiene nombres ni correos: la de
 * participaciones seudonimiza, la de entidades agrega por entidad.
 */
public interface CsvExportService {

    String exportParticipationsCsv(Integer year, String line);

    String exportPartnersCsv(Integer year);
}