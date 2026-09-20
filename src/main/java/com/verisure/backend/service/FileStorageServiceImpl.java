package com.verisure.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.verisure.backend.exception.UnsupportedMediaTypeException;

import lombok.extern.slf4j.Slf4j;

/**
 * Guarda los archivos en {@code uploads/<subfolder>/} y devuelve la URL relativa.
 *
 * <p>La subcarpeta va por dominio: {@code evidencias} para los cierres (B1-03).
 * La carpeta {@code uploads/} se crea al primer guardado; está en
 * {@code .gitignore} y la sirve {@code StaticResourceConfig} bajo
 * {@code /uploads/**}, ruta pública en lectura.
 *
 * <p>El nombre del archivo lo genera el servidor — un UUID — y la extensión sale
 * del tipo de contenido, nunca del nombre que manda el cliente.
 *
 * <p>Este servicio no valida tipo ni tamaño: la validación de la evidencia
 * (PDF · JPG · PNG, máximo 10 MB) vive en el llamador,
 * {@code ParticipationClosureServiceImpl.storeEvidence} (B1-03).
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    /** Carpeta de trabajo bajo la que se guardan los archivos. */
    private static final String UPLOADS_ROOT = "uploads";

    @Override
    public String store(MultipartFile file, String subfolder) {
        MediaType type = parseContentType(file);
        String extension = extensionFor(type);

        String filename = UUID.randomUUID() + "." + extension;
        Path target = Paths.get(UPLOADS_ROOT, subfolder, filename);

        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }

        log.info("Archivo guardado en {}", target);
        return "/" + UPLOADS_ROOT + "/" + subfolder + "/" + filename;
    }

    /** La extensión se deduce del tipo de contenido, no del nombre del cliente. */
    private String extensionFor(MediaType type) {
        if (type == null || type.getSubtype() == null) {
            throw new UnsupportedMediaTypeException("Tipo de archivo no admitido");
        }
        return switch (type.getSubtype().toLowerCase()) {
            case "jpeg" -> "jpg";
            case "png" -> "png";
            case "pdf" -> "pdf";
            default -> throw new UnsupportedMediaTypeException("Tipo de archivo no admitido");
        };
    }

    /** Un tipo malformado no puede derivar extensión y se traduce en 415. */
    private MediaType parseContentType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedMediaTypeException("El archivo no puede estar vacío");
        }
        try {
            return MediaType.parseMediaType(file.getContentType());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}