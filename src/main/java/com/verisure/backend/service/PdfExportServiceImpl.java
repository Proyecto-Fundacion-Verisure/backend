package com.verisure.backend.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.verisure.backend.dto.dashboard.DashboardResponse;
import com.verisure.backend.dto.dashboard.DepartmentEntry;
import com.verisure.backend.dto.dashboard.DistributionEntry;
import com.verisure.backend.dto.dashboard.EffectivenessMetric;
import com.verisure.backend.dto.dashboard.FavoriteRankingEntry;
import com.verisure.backend.dto.dashboard.ParticipationEntry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {

    private final DashboardService dashboardService;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDashboardPdf(Integer year, String line) {
        DashboardResponse data = dashboardService.getDashboard(year, line);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            header(document, data, year, line);

            document.add(new Paragraph("Indicadores", sectionFont()));
            document.add(kpisTable(data));

            document.add(new Paragraph("Eficacia", sectionFont()));
            document.add(effectivenessTable(data));

            if (!data.participationByDepartment().isEmpty()) {
                document.add(new Paragraph("Participación por departamento", sectionFont()));
                document.add(departmentsTable(data));
            }

            if (!data.participationByOrganization().isEmpty()) {
                document.add(new Paragraph("Participación por organización", sectionFont()));
                document.add(participationTable("Organización", data.participationByOrganization()));
            }

            if (!data.participationByLine().isEmpty()) {
                document.add(new Paragraph("Participación por línea de acción", sectionFont()));
                document.add(participationTable("Línea de acción", data.participationByLine()));
            }

            if (!data.distributionByMode().isEmpty()) {
                document.add(new Paragraph("Distribución por modalidad", sectionFont()));
                document.add(distributionTable("Modalidad", data.distributionByMode()));
            }

            if (!data.distributionByLocation().isEmpty()) {
                document.add(new Paragraph("Distribución por ubicación", sectionFont()));
                document.add(distributionTable("Ubicación", data.distributionByLocation()));
            }

            if (!data.favoriteRanking().isEmpty()) {
                document.add(new Paragraph("Ranking de actividades", sectionFont()));
                document.add(rankingTable(data));
            }

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el informe PDF", e);
        }
        return out.toByteArray();
    }

    private void header(Document document, DashboardResponse data, Integer year, String line) throws DocumentException {
        document.add(new Paragraph("Dashboard de impacto · Fundación Verisure", titleFont()));
        document.add(new Paragraph("Generado el " + data.generatedAt(), smallFont()));
        document.add(new Paragraph("Filtros: "
                + (year == null ? "todos los años" : "año " + year)
                + (line == null ? " · todas las líneas" : " · línea " + line),
                smallFont()));
    }

    private PdfPTable kpisTable(DashboardResponse data) {
        PdfPTable table = twoColumnTable();
        table.addCell(labelCell("Horas de voluntariado"));
        table.addCell(valueCell(String.valueOf(data.reportedHours())));
        table.addCell(labelCell("Voluntarios únicos"));
        table.addCell(valueCell(String.valueOf(data.activeVolunteers())));
        table.addCell(labelCell("Actividades finalizadas"));
        table.addCell(valueCell(String.valueOf(data.finishedActivities())));
        table.addCell(labelCell("Entidades colaboradoras"));
        table.addCell(valueCell(String.valueOf(data.activePartners())));
        return table;
    }

    private PdfPTable effectivenessTable(DashboardResponse data) {
        PdfPTable table = twoColumnTable();
        for (EffectivenessMetric metric : data.effectiveness()) {
            table.addCell(labelCell(metric.label()));
            table.addCell(valueCell(String.format("%.1f %%", metric.value())));
        }
        return table;
    }

    private PdfPTable departmentsTable(DashboardResponse data) {
        PdfPTable table = headerTable("Departamento", "Participantes");
        for (DepartmentEntry entry : data.participationByDepartment()) {
            table.addCell(valueCell(entry.department()));
            table.addCell(valueCell(String.valueOf(entry.participants())));
        }
        return table;
    }

    private PdfPTable participationTable(String header, List<ParticipationEntry> entries) {
        PdfPTable table = headerTable(header, "Participantes");
        for (ParticipationEntry entry : entries) {
            table.addCell(valueCell(entry.label()));
            table.addCell(valueCell(String.valueOf(entry.participants())));
        }
        return table;
    }

    private PdfPTable distributionTable(String header, List<DistributionEntry> entries) {
        PdfPTable table = headerTable(header, "%");
        for (DistributionEntry entry : entries) {
            table.addCell(valueCell(entry.label()));
            table.addCell(valueCell(String.format("%.1f %%", entry.value())));
        }
        return table;
    }

    private PdfPTable rankingTable(DashboardResponse data) {
        PdfPTable table = headerTable("Actividad", "Favoritos");
        for (FavoriteRankingEntry entry : data.favoriteRanking()) {
            table.addCell(valueCell(entry.activityTitle()));
            table.addCell(valueCell(String.valueOf(entry.favoriteCount())));
        }
        return table;
    }

    private PdfPTable twoColumnTable() {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        return table;
    }

    private PdfPTable headerTable(String first, String second) {
        PdfPTable table = twoColumnTable();
        table.addCell(labelCell(first));
        table.addCell(labelCell(second));
        return table;
    }

    private PdfPCell labelCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell valueCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private Font titleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.BOLD);
    }

    private Font sectionFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD);
    }

    private Font smallFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
    }
}