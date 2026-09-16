package com.verisure.backend.service;

/**
 * Etiquetas legibles de los valores en crudo que salen en el dashboard.
 *
 * <p>Las comparten el JSON, el CSV y el PDF para que los tres digan lo mismo.
 * Las de línea son las mismas que usa el frontend en {@code activityLines.js}.
 */
final class DashboardLabels {

    private DashboardLabels() {
    }

    static String mode(String mode) {
        return switch (mode) {
            case "PRESENCIAL" -> "Presencial";
            case "ONLINE" -> "Virtual";
            case "MIXTO" -> "Híbrida";
            default -> mode;
        };
    }

    static String modeId(String mode) {
        return switch (mode) {
            case "PRESENCIAL" -> "in-person";
            case "ONLINE" -> "virtual";
            case "MIXTO" -> "hybrid";
            default -> mode.toLowerCase();
        };
    }

    static String line(String line) {
        return switch (line) {
            case "desoledad" -> "Desoledad";
            case "educar" -> "Educar para proteger";
            case "acoso" -> "Protegidos ante el acoso";
            case "medioambiente" -> "Medio ambiente";
            default -> line;
        };
    }

    static String organization(String organization) {
        return switch (organization) {
            case "VERISURE_ES" -> "Verisure España";
            case "VERISURE_GROUP" -> "Verisure Group";
            default -> organization;
        };
    }
}
