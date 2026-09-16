package com.verisure.backend.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.verisure.backend.dto.dashboard.DashboardResponse;
import com.verisure.backend.service.CsvExportService;
import com.verisure.backend.service.DashboardService;
import com.verisure.backend.service.PdfExportService;

import lombok.RequiredArgsConstructor;

/**
 * Dashboard de la Fundación · {@code B1-07} · {@code B1-08} · {@code B1-10}.
 *
 * <p>Todas las rutas cuelgan de {@code /api/dashboard/**}, que la cadena de
 * seguridad ya reserva para {@code ADMIN}; el {@code @PreAuthorize} de la clase
 * es defensa en profundidad, no la barrera.
 *
 * <p>Los dos filtros, {@code year} y {@code line}, son opcionales y se combinan
 * con Y: los que no llegan no filtran nada.
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;

    @GetMapping
    public DashboardResponse dashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String line) {
        return dashboardService.getDashboard(year, line);
    }

    @GetMapping(path = "/participations.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> participationsCsv(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String line) {
        byte[] body = csvExportService.exportParticipationsCsv(year, line)
                .getBytes(StandardCharsets.UTF_8);
        return attachment("participations.csv", body, "text/csv;charset=UTF-8");
    }

    @GetMapping(path = "/partners.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> partnersCsv(
            @RequestParam(required = false) Integer year) {
        byte[] body = csvExportService.exportPartnersCsv(year)
                .getBytes(StandardCharsets.UTF_8);
        return attachment("partners.csv", body, "text/csv;charset=UTF-8");
    }

    @GetMapping(path = "/report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> reportPdf(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String line) {
        return attachment("dashboard-report.pdf",
                pdfExportService.exportDashboardPdf(year, line),
                MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<byte[]> attachment(String filename, byte[] body, String mediaType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}