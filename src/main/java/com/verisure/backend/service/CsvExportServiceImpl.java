package com.verisure.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.verisure.backend.repository.ParticipationClosureRepository;
import com.verisure.backend.repository.projection.DashboardClosedRow;
import com.verisure.backend.repository.projection.PartnerDashboardRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvExportServiceImpl implements CsvExportService {

    /** Marca de orden de bytes UTF-8: sin ella Excel abre los acentos rotos. */
    private static final String BOM = "\uFEFF";
    private static final String SEPARATOR = ";";
    private static final String LINE_BREAK = "\r\n";

    private final ParticipationClosureRepository participationClosureRepository;

    @Override
    @Transactional(readOnly = true)
    public String exportParticipationsCsv(Integer year, String line) {
        List<DashboardClosedRow> rows = participationClosureRepository.findDashboardData(year, line);

        StringBuilder csv = new StringBuilder(BOM)
                .append("id;actividad;linea;modalidad;horas;departamento;ubicacion;fecha")
                .append(LINE_BREAK);

        for (DashboardClosedRow row : rows) {
            csv.append(escape("P-" + row.closureId())).append(SEPARATOR)
                    .append(escape(row.activityTitle())).append(SEPARATOR)
                    .append(escape(row.line())).append(SEPARATOR)
                    .append(escape(toModeLabel(row.mode()))).append(SEPARATOR)
                    .append(row.actualHours() == null ? 0 : row.actualHours()).append(SEPARATOR)
                    .append(escape(row.department())).append(SEPARATOR)
                    .append(escape(row.location())).append(SEPARATOR)
                    .append(row.endDate())
                    .append(LINE_BREAK);
        }
        return csv.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportPartnersCsv(Integer year) {
        List<PartnerDashboardRow> rows = participationClosureRepository.findPartnersForDashboard(year);

        StringBuilder csv = new StringBuilder(BOM)
                .append("entidad;actividades;horas")
                .append(LINE_BREAK);

        for (PartnerDashboardRow row : rows) {
            csv.append(escape(row.partnerName())).append(SEPARATOR)
                    .append(row.activityCount() == null ? 0 : row.activityCount()).append(SEPARATOR)
                    .append(row.totalHours() == null ? 0 : row.totalHours())
                    .append(LINE_BREAK);
        }
        return csv.toString();
    }

    /** Envuelve en comillas lo que lleve separador o comillas, doblando estas. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(SEPARATOR) || value.contains("\"")
                || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String toModeLabel(String mode) {
        if (mode == null) {
            return "";
        }
        return switch (mode) {
            case "PRESENCIAL" -> "Presencial";
            case "ONLINE" -> "Virtual";
            case "MIXTO" -> "Híbrida";
            default -> mode;
        };
    }
}