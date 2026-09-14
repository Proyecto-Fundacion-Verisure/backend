package com.verisure.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.verisure.backend.exception.UnsupportedMediaTypeException;

import lombok.extern.slf4j.Slf4j;

/**
 * Guarda los archivos en {@code uploads/<subfolder>/} y devuelve la URL relativa.
 *
 * <p>La subcarpeta va por dominio: {@code portadas} para las actividades (B2-03) y
 * {@code evidencias} para los cierres (B1-03). La carpeta {@code uploads/} se
 * crea al primer guardado; está en {@code .gitignore} y la sirve
 * {@code StaticResourceConfig} bajo {@code /uploads/**}, ruta que pide token.
 *
 * <p>El nombre del archivo lo genera el servidor — un UUID — y la extensión sale
 * del tipo ya validado, nunca del nombre que manda el cliente.
 */
@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    /** El contrato limita las portadas a 5 MB (docs/api-contract.md §6.3). */
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final List<MediaType> SUPPORTED_IMAGE_TYPES =
            List.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG);

    /** Carpeta de trabajo bajo la que se guardan los archivos. */
    private static final String UPLOADS_ROOT = "uploads";

    @Override
    public String store(MultipartFile file, String subfolder) {
        MediaType type = validate(file);
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

    /**
     * Valida que el archivo no venga vacío, no supere 5 MB y sea JPG o PNG.
     * Devuelve el tipo ya validado para derivar la extensión sin tocar
     * el nombre original. 413 y 415 los traduce GlobalExceptionHandler.
     */
    private MediaType validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedMediaTypeException("La parte image no puede estar vacía");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_IMAGE_BYTES);
        }

        MediaType type = parseContentType(file);
        if (type == null || !isSupported(type)) {
            throw new UnsupportedMediaTypeException("Tipo de archivo no admitido");
        }
        return type;
    }

    private boolean isSupported(MediaType type) {
        return type != null &&
                SUPPORTED_IMAGE_TYPES.stream().anyMatch(supported -> supported.isCompatibleWith(type));
    }

    /** La extensión se deduce del tipo validado, no del nombre del cliente. */
    private String extensionFor(MediaType type) {
        if (type.isCompatibleWith(MediaType.IMAGE_JPEG)) {
            return "jpg";
        }
        return "png";
    }

    /** JPG/PNG son tipos seguros del resolver multipart; uno malformado no puede ser soportado. */
    private MediaType parseContentType(MultipartFile file) {
        try {
            return MediaType.parseMediaType(file.getContentType());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}